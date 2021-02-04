package cacophonia.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;


/**
  * {@link Agent} uses {@link Transformer} to implement the Java instrumentation API to 
  * trace all methods in Eclipse. 
  * <p>
  * Method calls between plugins are captured and sent to a remote UI to render, 
  * using {@link RemoteUI}.
  * <p>
  * See the following files for configuration:
  * <ul>
  * <li> <tt>build_agent.sh</tt>: A custom builder for this project. See <tt>Project Properties >Builders</tt>.
  * <li> <tt>manifest.txt</tt>: Used by <tt>build_agent.sh</tt> to create the agent jar.
  * <li> <tt>javassist.3.27.0.jar</tt>: The bytecode instrumentation packages used by the agent.
  * </ul>
  */
public class Agent {
	static String jarPath = System.getProperty("user.home") + "/cacophonia.1.0.0.jar";
	
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        try {
        	setup();
        	loadRemoteUI();
        	registerTransformer(instrumentation);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
    private static void registerTransformer(Instrumentation instrumentation) {
		System.out.println("Registering Cacophonia Transformer...");
        instrumentation.addTransformer(new Transformer());
	}

	private static void loadRemoteUI() throws Exception {
    	System.out.println("Cacophonia is now running...");
    	System.out.println("Loading Cacophonia UI...");
        ProcessBuilder process = new ProcessBuilder("java", "-classpath", jarPath, "cacophonia.ui.UI");
        process.inheritIO();
        process.start();
	}

	static void setup() {
    	if (!new File(jarPath).exists()) {
    		System.out.println("Cannot load Cacophonia. Please run 'build_agent.sh'. Missing: " + jarPath);
    		System.exit(1);
    	}
    }
}
