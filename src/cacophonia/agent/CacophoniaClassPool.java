package cacophonia.agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * 
 * A subclass of Javassist's ClassPool to handle package fragments.
 */
class CacophoniaClassPool extends ClassPool {
	ClassPool defaultClassPool = ClassPool.getDefault();
	
	public CacophoniaClassPool() {
	}
	
	@Override
	public CtClass get(String className) throws NotFoundException {
		if (isPackageFragment(className)) {
			throw new NotFoundException(className);
		}
		try {
			return super.get(className);
		} catch (NotFoundException e) {
			try {
				return defaultClassPool.get(className);
			} catch (NotFoundException ee) {
				// ee.printStackTrace();
			}
		}
		return null;
	}

	boolean isPackageFragment(String className) {
		if (className.startsWith("[") || className.endsWith("]")) return false;
		int lastDot = className.lastIndexOf(".");
		return lastDot == -1 || Character.isLowerCase(className.charAt(lastDot + 1));
	}
}