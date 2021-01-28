package cacophonia.runtime;

import java.lang.reflect.Field;
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
	Field fields[];

	private String plugin = "???";
	
	public static void enter(String methodName, Object object) {
		findMethod(methodName, object).enter(object);
	}

	public static void leave(String methodName, Object object) {
		findMethod(methodName, object).leave(object);
	}
		
	public Method(String name, Object object) {
		this.name = name;
		try {
			ClassLoader classLoader = object.getClass().getClassLoader();
			plugin = classLoader.toString().split("\\[")[1].split(":")[0];
			pluginNames.add(plugin);
			String[] nameParts = object.getClass().getName().split("\\.");
			fileName = nameParts[nameParts.length - 1].split("\\$")[0] + ".java";
			fields = object.getClass().getDeclaredFields();
		} catch(Exception e) {
			plugin = fileName = object.getClass().getName();
		}
	}
	
	public void enter(Object object) {
		methodCallCount++;
		synchronized (tracedPlugins) {
			if (tracedPlugins.contains(plugin)) {
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
			if (!lastPlugin.equals(plugin)) {
				Stack<String> stack = pluginStack.get();
				stack.push(plugin);
				remoteUI.sendEvent(Constants.PLUGIN_TO_PLUGIN_CALL, String.format("%s %s", lastPlugin, plugin));
				totalPluginCallCount++;
			}
			lastPlugin = plugin;
			if (++totalMethodCallCount % 10000 == 0) {
				String stats = String.format("#method=%,d  #messages=%,d  #plugins=%,d",
						totalMethodCallCount, totalPluginCallCount, pluginNames.size()
				);
				remoteUI.sendEvent(Constants.STATISTICS, stats);
			}
		}
	}
	
	public void leave(Object object) {
		synchronized (tracedPlugins) {
			if (tracedPlugins.contains(plugin)) {
				callDepth = Math.max(0,  callDepth - 1);
				if (callDepth > 32) callDepth = 0;
				for (int n=0; n<callDepth; n++) System.out.print("    ");
				System.out.println(String.format("}"));
			}
			if (lastPlugin.equals(plugin)) {
				Stack<String> stack = pluginStack.get();
				if (!stack.isEmpty()) lastPlugin = stack.pop();
			}
		}
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