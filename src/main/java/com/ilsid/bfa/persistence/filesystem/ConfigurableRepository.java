package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.util.Map;

import javax.inject.Inject;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.persistence.RepositoryConfig;

public abstract class ConfigurableRepository implements Configurable {

	private static final String CONFIG_PROP_ROOT_DIR_NAME = "bfa.persistence.fs.root_dir";

	protected String rootDir;

	/**
	 * Defines the configuration for the file system repository.
	 * 
	 * @param config
	 *            the configuration that must contain <code>bfa.persistence.fs.root_dir</code> property set to the valid
	 *            directory path
	 * @throws ConfigurationException
	 *             if the configuration is invalid
	 */
	@Override
	@Inject
	public void setConfiguration(@RepositoryConfig Map<String, String> config) throws ConfigurationException {
		rootDir = config.get(CONFIG_PROP_ROOT_DIR_NAME);
		if (rootDir == null) {
			throw new ConfigurationException("Required [" + CONFIG_PROP_ROOT_DIR_NAME + "] property not found");
		}

		if (!new File(rootDir).isDirectory()) {
			throw new ConfigurationException("[" + rootDir + "] value defined by [" + CONFIG_PROP_ROOT_DIR_NAME
					+ "] property is not a directory");
		}
	}

}
