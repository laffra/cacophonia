package cacophonia.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;


class PluginRegistryTest {

	@Test
	void test_rcp_exists() {
		Set<String> features = PluginRegistry.getAllFeatures();
		assertTrue(features.contains("org.eclipse.rcp"));
	}

	@Test
	void test_ui_exists() {
		Set<String> plugins = PluginRegistry.getAllPlugins();
		assertTrue(plugins.contains("org.eclipse.ui"));
	}

	@Test
	void test_internal_exists() {
		Set<String> fragments = PluginRegistry.getAllFragments();
		assertTrue(fragments.contains("org.eclipse.ui.internal"));
	}

	@Test
	void test_rcp_contains_ui() {
		Set<String> plugins = PluginRegistry.getPluginsForFeature("org.eclipse.rcp");
		assertTrue(plugins.contains("org.eclipse.ui"));
	}
	
	@Test
	void test_ui_contains_internal() {
		Set<String> fragments = PluginRegistry.getFragmentsForPlugin("org.eclipse.ui");
		assertTrue(fragments.contains("org.eclipse.ui.internal"));
		assertTrue(fragments.contains("org.eclipse.ui.internal.cocoa"));
	}
	
	@Test
	void test_internal_belongs_to_ui() {
		String plugin = PluginRegistry.getPluginForFragment("org.eclipse.ui.internal");
		assertEquals(plugin, "org.eclipse.ui");
	}
	
	@Test
	void test_ui_belongs_to_rcp() {
		String feature = PluginRegistry.getFeatureForPlugin("org.eclipse.ui");
		assertEquals(feature, "org.eclipse.rcp");
	}
	
	void dump() {
		for (String feature: PluginRegistry.getAllFeatures()) {
			System.out.println(feature);
			for (String plugin: PluginRegistry.getPluginsForFeature(feature)) {
				System.out.println("    " + plugin);
				for (String fragment: PluginRegistry.getFragmentsForPlugin(plugin)) {
					System.out.println("        " + fragment);
				}
			}
		}
	}

}
