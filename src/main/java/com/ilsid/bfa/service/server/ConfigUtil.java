package com.ilsid.bfa.service.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides configuration related functionality.
 * 
 * @author illia.sydorovych
 *
 */
public class ConfigUtil {

	private static final File CONFIG_FILE = new File("bfa/config.properties");

	/**
	 * Loads configuration from <i>&lt;current_dir&gt;/bfa/config.properties<i> file.
	 * 
	 * @return return the loaded configuration
	 * @throws IllegalStateException
	 *             if the configuration file is not found or can't be loaded
	 */
	public static Map<String, String> getApplicationSettings() {
		return load();
	}

	private static Map<String, String> load() {
		Map<String, String> result;
		Properties props = new Properties();

		try (InputStream configFile = new FileInputStream(CONFIG_FILE)) {
			props.load(configFile);
			result = new HashMap<String, String>();
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(String.format("BFA configuration file [%s] is not found", CONFIG_FILE));
		} catch (IOException e) {
			throw new IllegalStateException(String.format("Failed to load BFA configuration file [%s]", CONFIG_FILE),
					e);
		}

		for (String key : props.stringPropertyNames()) {
			result.put(key, props.getProperty(key));
		}

		return result;
	}

}
