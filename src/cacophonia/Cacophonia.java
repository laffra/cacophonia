package cacophonia;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Stack;

/**
 * The Cacophonia runtime that runs inside the instrumentation agent, as part of the Eclipse process.
 * 
 * @author Chris Laffra
 *
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
 * 
 * @author Chris Laffra
 *
 */
class Method {
	private static Hashtable<String,Method> methods = new Hashtable<String,Method>();
	
	private static ThreadLocal<Stack<String>> pluginStack = ThreadLocal.withInitial(() -> new Stack<String>());
	private static String lastPlugin = "eclipse.main";
	private static RemoteUI remoteUI = new RemoteUI();
	
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
		if (Cacophonia.debug) System.out.println(String.format("> %d %s %s", callCount, plugin, name));
		if (!lastPlugin.equals(plugin)) {
			Stack<String> stack = pluginStack.get();
			stack.push(plugin);
			remoteUI.sendEvent(String.format("%s %s", lastPlugin, plugin));
		}
		lastPlugin = plugin;
	}
	
	public void leave() {
		if (Cacophonia.debug) System.out.println(String.format("< %d %s %s", callCount, plugin, lastPlugin, name));
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
}


/**
 * Notifies the remote UI, running in a different process, that a call is made between two plugins.
 * 
 * See {@link cacophonia.UI} for the implementation of the remote UI itself.
 * 
 * @author Chris Laffra
 *
 */
class RemoteUI {
	Socket socket;
	DataOutputStream stream;
	
	public RemoteUI() {
		try {
			socket = new Socket("localhost",6666);
			stream = new DataOutputStream(socket.getOutputStream());  
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}  
	}
	
	public void sendEvent(String message) {
		try {
			synchronized (socket) {
				stream.writeUTF(message);
				stream.flush(); 
			}
		} catch (Exception e) {
			if (Cacophonia.debug) e.printStackTrace();
		}  
	}
	
	void close() {
		try {
			stream.close();
			socket.close();   
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
}

