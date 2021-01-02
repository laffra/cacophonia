package cacophonia;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;


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
  * 
  * @author Chris Laffra
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

	private static void loadRemoteUI() throws IOException {
    	System.out.println("Cacophonia is now running...");
    	System.out.println("Loading Cacophonia UI...");
        new ProcessBuilder("java", "-classpath", jarPath, "cacophonia.UI").start();
	}

	static void setup() {
    	if (!new File(jarPath).exists()) {
    		System.out.println("Cannot load Cacophonia. Please run 'build_agent.sh'. Missing: " + jarPath);
    		System.exit(1);
    	}
    }
}


/**
 * Transformer instruments each method with calls to the Cacopohonia runtime to inform it on enter/leave events.
 *  
 * @author Chris Laffra
 *
 */
class Transformer implements ClassFileTransformer {
	int classLoadCount;
	HashSet<ClassLoader> classLoaders = new HashSet<ClassLoader>();
	boolean  debug = false;
	
    @Override
    public byte[] transform(ClassLoader classLoader, String rawClassName, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        String className = rawClassName.replace("/", ".");
        trackStatistics(classLoader, className);
    	if (isInstrumentable(className)) {
    		classfileBuffer = instrument(className, classfileBuffer);
        }
        return classfileBuffer;
    }
    
    private byte[] instrument(String className, byte[] classfileBuffer) {
    	try {
            CacophoniaClassPool classPool = new CacophoniaClassPool();
            classPool.insertClassPath(new ByteArrayClassPath(className, classfileBuffer));
            CtClass classDefinition = classPool.get(className);
            if (classDefinition.isInterface()) {
            	return classfileBuffer;
            }
            for (CtMethod method : classDefinition.getDeclaredMethods()) {
            	if (method.getMethodInfo().getCodeAttribute() != null) {
            		instrumentMethod(className, method);
            	}
            }
            classfileBuffer = classDefinition.toBytecode();
            classDefinition.detach();
        } catch (Exception e) {
            if (debug) e.printStackTrace();
        }
		return classfileBuffer;
	}

	private void trackStatistics(ClassLoader classLoader, String className) {
    	if (!classLoaders.contains(classLoader)) {
        	String name = classLoader.toString();
        	if (name.startsWith("org.eclipse.")) {
        		classLoaders.add(classLoader);
        		System.out.println(String.format("Load plugin %d for %s", classLoaders.size(), className));
        	}
        }
        if ((++classLoadCount % 500) == 0) {
        	System.out.println(String.format("Loaded %d classes", classLoadCount));
        }
    }

	private void instrumentMethod(String className, CtMethod method) throws CannotCompileException {
		try {
			method.insertBefore("cacophonia.Cacophonia.enter(\"" + method.getLongName() + "\", $0);");    	
			method.insertAfter("cacophonia.Cacophonia.leave(\"" + method.getLongName() + "\", $0);");    	
		} catch (Exception e) {
            if (debug) System.out.println("Cannot instrument "+method.getName()+" "+method.getSignature());
        }
	}

	private boolean isInstrumentable(String className) {
		if (isJavaClass(className) || className.startsWith("cacophonia.")) return false;
		if (className.startsWith("org.eclipse.osgi.")) return false;
		if (className.startsWith("org.eclipse.equinox.")) return false;
		if (className.startsWith("org.apache.felix.")) return false;
		if (className.startsWith("org.osgi.")) return false;
		return true;
	}

	private boolean isJavaClass(String className) {
		return (className.startsWith("jdk.") || 
				className.startsWith("java.") || 
				className.startsWith("sun.") || 
				className.startsWith("com.sun.") || 
				className.startsWith("javax."));
	}
}

/**
 * 
 * A subclass of Javassist's ClassPool to handle package fragments.
 * 
 * @author Chris Laffra
 *
 */
class CacophoniaClassPool extends ClassPool {
	ClassPool defaultClassPool = ClassPool.getDefault();

	@Override
	public CtClass get(String className) throws NotFoundException {
		if (isPackageFragment(className)) {
			throw new NotFoundException(className);
		}
		try {
			return defaultClassPool.get(className);
		} catch (NotFoundException e) {
			return super.get(className);
		}
	}

	private boolean isPackageFragment(String className) {
		if (className.startsWith("[") || className.endsWith("]")) return false;
		int lastDot = className.lastIndexOf(".");
		return lastDot == -1 || Character.isLowerCase(className.charAt(lastDot + 1));
	}
}
