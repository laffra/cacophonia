package cacophonia.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * Transformer instruments each method with calls to the Cacopohonia runtime to inform it on enter/leave events.
 */
class Transformer implements ClassFileTransformer {
	static String HOME_DIR = System.getProperty("user.home") + "/.cacophonia";
	int classLoadCount;
	HashSet<ClassLoader> classLoaders = new HashSet<ClassLoader>();
	boolean debug = false;
	CacophoniaClassPool classPool = new CacophoniaClassPool();
	long startMillis = System.currentTimeMillis();
	
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
    
    byte[] instrument(String className, byte[] classfileBuffer) {
    	try {
    		return getFromCache(className);
    	} catch (IOException e) {
    		// not in cache
    	}
    	try {
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
		try {
			putInCache(className, classfileBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return classfileBuffer;
	}
    
    byte[] getFromCache(String className) throws IOException {
		return Files.readAllBytes(getCacheFile(className).toPath());
    }
    
    void putInCache(String className, byte[] bytes) throws IOException {
		Files.write(getCacheFile(className).toPath(), bytes);
    }
    
    File getCacheFile(String className) {
    	File file = new File(String.format("%s/%s.class", HOME_DIR, className.replace(".", "/")));
    	if (!file.exists()) {
    		file.getParentFile().mkdirs();
    	}
		return file;
    }

	void trackStatistics(ClassLoader classLoader, String className) {
		long seconds = (System.currentTimeMillis() - startMillis) / 1000;
    	if (!classLoaders.contains(classLoader)) {
        	String name = classLoader.toString();
        	if (name.startsWith("org.eclipse.")) {
        		classLoaders.add(classLoader);
        		System.out.println(String.format("%s %ds Load plugin %d - %s", when(), seconds,
        				classLoaders.size(), className));
        	}
        }
        if ((++classLoadCount % 500) == 0) {
        	System.out.println(String.format("%s %ds Loaded %d classes", when(), seconds, classLoadCount));
        }
    }
	
	String when() {
		return new SimpleDateFormat("HH:mm:ss").format(new Date());
	}

	void instrumentMethod(String className, CtMethod method) throws CannotCompileException {
		try {
			instrumentEnterLeave(method, "$0");
		} catch (Exception e1) {
			try {
				instrumentEnterLeave(method, "null");
			} catch (Exception e2) {
				if (debug) System.out.println("Cannot instrument " + e2 + ": " + className + " " + method.getName());
			}
        }
	}
	
	void instrumentEnterLeave(CtMethod method, String self) throws CannotCompileException {
		String enter = String.format("cacophonia.runtime.Cacophonia.enter(\"%s\", %s);", method.getLongName(), self);    	
		method.insertBefore(enter);
		String leave = String.format("cacophonia.runtime.Cacophonia.leave(\"%s\", %s);", method.getLongName(), self);    	
		method.insertAfter(leave);
	}

	boolean isInstrumentable(String className) {
		if (isJavaClass(className) || className.startsWith("cacophonia.")) return false;
		if (className.startsWith("org.eclipse.osgi.")) return false;
		if (className.startsWith("org.eclipse.equinox.")) return false;
		if (className.startsWith("org.apache.felix.")) return false;
		if (className.startsWith("org.osgi.")) return false;
		return true;
	}

	boolean isJavaClass(String className) {
		return (className.startsWith("jdk.") || 
				className.startsWith("java.") || 
				className.startsWith("sun.") || 
				className.startsWith("com.sun.") || 
				className.startsWith("javax."));
	}
}