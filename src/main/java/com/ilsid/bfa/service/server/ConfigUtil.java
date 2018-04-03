package com.ilsid.bfa.service.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.ilsid.bfa.common.LoggingConfigurator;

/**
 * Provides configuration related functionality.
 * 
 * @author illia.sydorovych
 *
 */
public class ConfigUtil {

	private static final String CONFIG_SYS_PROPERTY_NAME = "bfa.config";

	private static final String LOGGING_SYS_PROPERTY_NAME = "bfa.log";

	private static final String DEFAULT_CONFIG_FILE = "bfa/config.properties";

	private static Map<String, String> config;

	/**
	 * Loads configuration from a file defined by <i>bfa.config</i> system property. If such property is not defined then
	 * configuration is loaded from <i>bfa/config.properties<i> file.
	 * 
	 * @return return the loaded configuration
	 * @throws IllegalStateException
	 *             if the configuration file is not found or can't be loaded
	 */
	public static synchronized Map<String, String> getApplicationSettings() {
		if (config == null) {
			config = load();
			configureLogging();
		}

		return config;
	}

	private static Map<String, String> load() {
		String configFilePath = System.getProperty(CONFIG_SYS_PROPERTY_NAME, DEFAULT_CONFIG_FILE);
		File configFile = new File(configFilePath);

		Properties props = new Properties();

		try (InputStream configFileStream = new FileInputStream(configFile)) {
			props.load(configFileStream);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(String.format("BFA configuration file [%s] is not found", configFile));
		} catch (IOException e) {
			throw new IllegalStateException(String.format("Failed to load BFA configuration file [%s]", configFile), e);
		}

		Map<String, String> result = new HashMap<String, String>();
		for (String key : props.stringPropertyNames()) {
			result.put(key, props.getProperty(key));
		}

		return result;
	}

	private static void configureLogging() {
		String configFilePath = System.getProperty(LOGGING_SYS_PROPERTY_NAME);
		if (configFilePath != null) {
			LoggingConfigurator.configureLog4j(configFilePath);
		}
	}

}
