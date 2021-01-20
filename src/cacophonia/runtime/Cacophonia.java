package cacophonia.runtime;

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
