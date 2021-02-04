package cacophonia.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cacophonia.DetailLevel;

public class PluginRegistry {
	static final Set<String> EMPTY_SET = new HashSet<String>();
	static final Map<String,String>pluginToFeature = new HashMap<String,String>();
	static final Map<String,Set<String>>featureToPlugins = new HashMap<String,Set<String>>();
	static final Map<String,String>fragmentToPlugin = new HashMap<String,String>();
	static final Map<String,Set<String>>pluginToFragments = new HashMap<String,Set<String>>();

	static {
		loadFeatures();
		loadPlugins();
	}
	
	static void loadFeatures() {
		File file = new File(String.format("%s/.p2/pool/features", System.getProperty("user.home")));
		try {
			Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.getFileName().toString().equals("feature.xml")) {
						loadFeature(file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void loadFeature(Path file) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file.toFile());
			String featureName = doc.getDocumentElement().getAttribute("id");
			NodeList plugins = doc.getElementsByTagName("plugin");
			for (int n=0; n<plugins.getLength(); n++) {
				Element plugin = (Element)plugins.item(n);
				String pluginName = plugin.getAttribute("id");
				addPlugin(featureName, pluginName);
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}

	static void addPlugin(String featureName, String pluginName) {
		Set<String> plugins = featureToPlugins.get(featureName);
		if (plugins == null) {
			plugins = new HashSet<String>();
			featureToPlugins.put(featureName, plugins);
		}
		plugins.add(pluginName);
		pluginToFeature.put(pluginName, featureName);
		pluginToFragments.put(pluginName, new HashSet<String>());
	}

	static void addFragment(String pluginName, String fragmentName) {
		Set<String> fragments = pluginToFragments.get(pluginName);
		if (fragments == null) {
			fragments = new HashSet<String>();
			pluginToFragments.put(pluginName, fragments);
		}
		fragments.add(fragmentName);
		fragmentToPlugin.put(fragmentName, pluginName);
	}

	public static String getFeatureForPlugin(String pluginName) {
		if (!pluginToFeature.containsKey(pluginName) || pluginToFeature.get(pluginName) == null) {
			addPlugin(pluginName, pluginName);
		}
		return pluginToFeature.get(pluginName);
	}

	public static Set<String> getPluginsForFeature(String featureName) {
		return featureToPlugins.getOrDefault(featureName, EMPTY_SET);
	}

	public static String getPluginForFragment(String fragmentName) {
		if (!fragmentToPlugin.containsKey(fragmentName) || fragmentToPlugin.get(fragmentName) == null) {
			addFragment(fragmentName, fragmentName);
		}
		return fragmentToPlugin.get(fragmentName);
	}

	public static Set<String> getFragmentsForPlugin(String pluginName) {
		return pluginToFragments.getOrDefault(pluginName, EMPTY_SET);
	}
	
	static void loadPlugins() {
		File file = new File(String.format("%s/.p2/pool/plugins", System.getProperty("user.home")));
		try {
			Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.getFileName().toString().endsWith(".jar")) {
						loadPlugin(file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void loadPlugin(Path file) throws IOException {
		String fileName = file.toFile().getAbsolutePath();
		ZipFile zipFile = new ZipFile(fileName);
	    Enumeration<? extends ZipEntry> entries = zipFile.entries();
	    while(entries.hasMoreElements()) {
	        ZipEntry entry = entries.nextElement();
	        if (entry.getName().equals("META-INF/MANIFEST.MF")) {
	        	Manifest manifest = new Manifest(zipFile.getInputStream(entry));
	        	Attributes attributes = manifest.getMainAttributes();
	        	String pluginName = getAttribute(attributes, "Bundle-SymbolicName").replaceAll(";.*", "").strip();
				String fragmentHostName = getAttribute(attributes, "Fragment-Host").replaceAll(";.*", "").strip();
				String exports[] = getAttribute(attributes, "Export-Package").split(",");
				if (fragmentHostName.length() > 0) {
					addFragment(pluginName, fragmentHostName);
					pluginName = fragmentHostName;
				}
				for (String export : exports) {
					if (export.length() == 0) continue;
					String packageName = export.replaceAll(";.*", "").replaceAll("\"", "").strip();
					addFragment(pluginName, packageName);
				}
	        }
	    }
	    zipFile.close();
	}

	static String getAttribute(Attributes attributes, String name) {
		String value = attributes.getValue(name);
		if (value == null) return "";
		return value;
	}

	public static Set<String> getFamilyForFragment(String fragmentName) {
		String pluginName = PluginRegistry.getPluginForFragment(fragmentName);
		return PluginRegistry.getFragmentsForPlugin(pluginName);
	}

	public static Set<String> getFamilyForPlugin(String pluginName) {
		String featureName = PluginRegistry.getFeatureForPlugin(pluginName);
		return PluginRegistry.getPluginsForFeature(featureName);
	}

	public static Set<String> getAllFragments(String name, DetailLevel detailLevel) {
		Set<String> names = new HashSet<String>();
		switch (detailLevel) {
		case FEATURE:
			for (String plugin : featureToPlugins.get(name)) {
				names.addAll(getAllFragments(plugin, DetailLevel.PLUGIN));
			}
			break;
		case PLUGIN:
			names.add(name);
			for (String fragment : pluginToFragments.get(name)) {
				names.addAll(getAllFragments(fragment, DetailLevel.FRAGMENT));
			}
			break;
		case FRAGMENT:
			names.add(name);
			break;
		}
		return names;
	}

	public static Set<String> getAllFeatures() {
		return featureToPlugins.keySet();
	}

	public static Set<String> getAllPlugins() {
		return pluginToFeature.keySet();
	}

	public static Set<String> getAllFragments() {
		return fragmentToPlugin.keySet();
	}
}
