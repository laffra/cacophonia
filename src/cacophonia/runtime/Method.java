package cacophonia.runtime;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

import cacophonia.Constants;

/**
 * Represents a method inside a class defined by one of Eclipse's plugins. 
 * 
 * Keeps track of callstacks to determine whether a call is made from one plugin to another. In that case,
 * {@link RemoteUI} is used to send an event to the UI that runs in a different process.
 */
class Method {
	private static Hashtable<String,Method> methods = new Hashtable<String,Method>();
	private static ThreadLocal<Stack<String>> pluginStack = ThreadLocal.withInitial(() -> new Stack<String>());
	private static String lastPlugin = "eclipse.main";
	private static RemoteUI remoteUI = new RemoteUI();
	private static int callDepth = 0;
	private static HashSet<String> pluginNames = new HashSet<String>();
	private static HashSet<String> tracedPlugins = new HashSet<String>();

	String name;
	String fileName = "???";
	int methodCallCount;
	static int totalPluginCallCount;
	static int totalMethodCallCount;
	Field fields[] = {};

	private String pluginName = "???";
	private Field jobNameField;
	private double jobStartTime;
	static int jobCount;
	
	public static void enter(String methodName, Object object) {
		findMethod(methodName, object).enter(object);
	}

	public static void leave(String methodName, Object object) {
		findMethod(methodName, object).leave(object);
	}
		
	public Method(String name, Object object) {
		this.name = name;
		if (object == null) {
			// handle static method, which does not have an object to get the class from. Strip off class/method
			pluginName = name.substring(0, name.lastIndexOf("("));
			pluginName = pluginName.substring(0, pluginName.lastIndexOf("."));
			pluginName = pluginName.substring(0, pluginName.lastIndexOf("."));
			return;
		}
		try {
			ClassLoader classLoader = object.getClass().getClassLoader();
			try {
				pluginName = classLoader.toString().split("\\[")[1].split(":")[0];
			} catch (Exception e) {
				pluginName = classLoader.toString();
			}
			String[] nameParts = object.getClass().getName().split("\\.");
			fileName = nameParts[nameParts.length - 1].split("\\$")[0] + ".java";
			fields = object.getClass().getDeclaredFields();
			jobNameField = getJobNameField(object);
			if (!pluginNames.contains(pluginName)) {
				sendPlugin(pluginName, classLoader);
			}
		} catch(Exception e) {
			e.printStackTrace();
			pluginName = fileName = object.getClass().getName();
		}
		pluginNames.add(pluginName);
	}
	
	private void sendPlugin(String pluginName, ClassLoader classLoader) throws SecurityException, IllegalArgumentException, IllegalAccessException {
		try {
			// classLoader is an instance of EquinoxClassLoader
			Field managerField = classLoader.getClass().getDeclaredField("manager");
			managerField.setAccessible(true);
			Object manager = managerField.get(classLoader);
			// manager is an instance of ClasspathManager

			Field entriesField = manager.getClass().getDeclaredField("entries");
			entriesField.setAccessible(true);
			Object entries[] = (Object[])entriesField.get(manager);
			// entries is an array of ClasspathEntry objects

			for (Object entry : entries) {
				Field bundlefileField = entry.getClass().getDeclaredField("bundlefile");
				bundlefileField.setAccessible(true);
				Object bundlefile = bundlefileField.get(entry);
				// bundleFile is an instance of BundleFile
				remoteUI.sendEvent(Constants.EVENT_PLUGIN_DETAILS, String.format("%s %s", pluginName, bundlefile.toString()));
			}
		} catch (NoSuchFieldException e) {
			return; // this is not an EquinoxClassLoader
		}
	}

	Field getJobNameField(Object object) {
		if (!object.getClass().getSuperclass().getName().equals("org.eclipse.core.runtime.jobs.Job")) return null;
		Class<? extends Object> clazz = object.getClass();
		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField("name");
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException e) {
				// ignore
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}
	
	public void enter(Object object) {
		methodCallCount++;
		synchronized (tracedPlugins) {
			if (tracedPlugins.contains(pluginName)) {
				if (callDepth == 0) System.out.println(new Date());
				for (int n=0; n<callDepth; n++) System.out.print("    ");
				System.out.println(String.format("at %s(%s:1) - %d calls {", name, fileName, methodCallCount));
				callDepth++;
				for (Field field : fields) {
				    field.setAccessible(true);
				    Object value;
					try {
						value = field.get(object);
						Object printableValue = field.getType().isPrimitive() ? value : object.getClass().getName();
						String fieldDetail = field.getName() + "=" + printableValue;
				    	for (int n=0; n<callDepth; n++) System.out.print("    ");
						System.out.println(fieldDetail);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						// ignore
					} 
				}
			}
			if (!lastPlugin.equals(pluginName)) {
				Stack<String> stack = pluginStack.get();
				stack.push(pluginName);
				remoteUI.sendEvent(Constants.EVENT_PLUGIN_TO_PLUGIN_CALL, String.format("%s %s", lastPlugin, pluginName));
				totalPluginCallCount++;
			}
			lastPlugin = pluginName;
			if (++totalMethodCallCount % 100000 == 0) {
				String maxMemory = Util.formatSize(Runtime.getRuntime().maxMemory());
				String totalMemory = Util.formatSize(Runtime.getRuntime().totalMemory());

				String stats = String.format("memory=%s/%s  #method=%,d  #messages=%,d  #plugins=%,d",
						totalMemory,
						maxMemory,
						totalMethodCallCount,
						totalPluginCallCount,
						pluginNames.size()
				);
				remoteUI.sendEvent(Constants.EVENT_STATISTICS, stats);
			}
			if (jobNameField != null) {
				jobStartTime = System.currentTimeMillis();
			}
		}
	}

	public void leave(Object object) {
		synchronized (tracedPlugins) {
			if (tracedPlugins.contains(pluginName)) {
				callDepth = Math.max(0,  callDepth - 1);
				if (callDepth > 32) callDepth = 0;
				for (int n=0; n<callDepth; n++) System.out.print("    ");
				System.out.println(String.format("}"));
			}
			if (lastPlugin.equals(pluginName)) {
				Stack<String> stack = pluginStack.get();
				if (!stack.isEmpty()) lastPlugin = stack.pop();
			}
			if (jobNameField != null && name.endsWith(".run(org.eclipse.core.runtime.IProgressMonitor)")) {
				try {
					String name = (String) jobNameField.get(object);
					String message = String.format("%05d %s, %.1fs \"%s\"    (%s)",
							jobCount++,
							when(),
							(System.currentTimeMillis() - jobStartTime) / 1000,
							name,
							object.getClass().getName());
					System.out.println("#### Run Job " + message);
					remoteUI.sendEvent(Constants.EVENT_JOB, message);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private String when() {
		return new SimpleDateFormat("HH:mm:ss").format(new Date());
	}

	private static Method findMethod(String methodName, Object object) {
		Method method = methods.get(methodName);
		if (method == null) {
			method = new Method(methodName, object);
			methods.put(methodName, method);
		}
		return method;
	}

	public static void trace(String plugin, boolean trace) {
		if (trace) {
			tracedPlugins.add(plugin);
			System.out.println("####### Enable tracing for " + plugin);
		} else {
			tracedPlugins.remove(plugin);
			System.out.println("####### Disable tracing for " + plugin);
		}
		Method.callDepth = 0;
	}
}