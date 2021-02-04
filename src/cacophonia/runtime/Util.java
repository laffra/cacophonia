package cacophonia.runtime;

public class Util {

	public static String formatSize(long v) {
	    if (v < 1024) return v + " B";
	    int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
	    return String.format("%.0f%sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
	}

}
