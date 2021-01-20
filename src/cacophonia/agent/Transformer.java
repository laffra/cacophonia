package cacophonia.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashSet;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * Transformer instruments each method with calls to the Cacopohonia runtime to inform it on enter/leave events.
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
			method.insertBefore("cacophonia.runtime.Cacophonia.enter(\"" + method.getLongName() + "\", $0);");    	
			method.insertAfter("cacophonia.runtime.Cacophonia.leave(\"" + method.getLongName() + "\", $0);");    	
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