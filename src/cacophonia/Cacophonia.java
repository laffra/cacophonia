package cacophonia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

/**
 * The Cacophonia runtime that runs inside the instrumentation agent, as part of the Eclipse process.
 */
public class Cacophonia {
	static boolean debug = false;

	public static void enter(String methodName, Object object) {
		Method.enter(methodName, object);
	}

	public static void leave(String methodName, Object object) {
		Method.leave(methodName, object);
	}
}


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
	
	static HashSet<String> tracedPlugins = new HashSet<String>();
	String name;
	int callCount;

	private String plugin;
	
	public static void enter(String methodName, Object object) {
		findMethod(methodName, object).enter();
	}

	public static void leave(String methodName, Object object) {
		findMethod(methodName, object).leave();
	}
		
	public Method(String name, Object object) {
		this.name = name;
		try {
			ClassLoader classLoader = object.getClass().getClassLoader();
			plugin = classLoader.toString().split("\\[")[1].split(":")[0];
		} catch(Exception e) {
			plugin = object.getClass().getName();
		}
	}
	
	public void enter() {
		callCount++;
		synchronized (tracedPlugins) {
			if (tracedPlugins.contains(plugin)) {
				for (int n=0; n<callDepth; n++) System.out.print("    ");
				System.out.println(String.format("%s (%d calls) {", name, callCount));
				callDepth++;
			}
		}
		if (!lastPlugin.equals(plugin)) {
			Stack<String> stack = pluginStack.get();
			stack.push(plugin);
			remoteUI.sendEvent(String.format("%s %s", lastPlugin, plugin));
		}
		lastPlugin = plugin;
	}
	
	public void leave() {
		synchronized (tracedPlugins) {
			if (tracedPlugins.contains(plugin)) {
				callDepth = Math.max(0,  callDepth - 1);
				if (callDepth > 32) callDepth = 0;
				for (int n=0; n<callDepth; n++) System.out.print("    ");
				System.out.println(String.format("}"));
			}
		}
		if (lastPlugin.equals(plugin)) {
			Stack<String> stack = pluginStack.get();
			if (!stack.isEmpty()) lastPlugin = stack.pop();
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


/**
 * Notifies the remote UI, running in a different process, that a call is made between two plugins.
 * 
 * See {@link cacophonia.UI} for the implementation of the remote UI itself.
 *
 */
class RemoteUI {
	Socket socket;
	DataInputStream inputStream;
	DataOutputStream outputStream;
	
	public RemoteUI() {
		try {
			socket = new Socket("localhost",6666);
			inputStream = new DataInputStream(socket.getInputStream());  
			outputStream = new DataOutputStream(socket.getOutputStream());  
			this.setupListener();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}  
	}
	
	private void setupListener() {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						int command = inputStream.readInt();
						String details = (String)inputStream.readUTF();
						switch (command) {
						case Constants.INSPECT_PLUGIN:
							Method.trace(details, true);
							break;
						case Constants.UN_INSPECT_PLUGIN:
							Method.trace(details, false);
							break;
						case Constants.IMPORT_PLUGIN_FROM_SOURCE:
						case Constants.IMPORT_PLUGIN_FROM_REPOSITORY:
							System.out.println("Importing of plugins is not yet implemented.");
							System.out.println("Please import by hand: " + details);
							break;
						case Constants.EXIT:
							System.exit(0);
							break;
						}
					} catch (Exception e) {
						System.err.println(e);
						System.exit(1);
					}
				}
			}
		}).start();
	}
	
	public void sendEvent(String message) {
		try {
			synchronized (socket) {
				outputStream.writeUTF(message);
				outputStream.flush(); 
			}
		} catch (Exception e) {
			if (Cacophonia.debug) e.printStackTrace();
		}  
	}
	
	void close() {
		try {
			outputStream.close();
			socket.close();   
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
}

