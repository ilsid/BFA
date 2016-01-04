package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.persistence.RepositoryConfig;

public abstract class ConfigurableRepository implements Configurable {

	private static final String CONFIG_PROP_ROOT_DIR_NAME = "bfa.persistence.fs.root_dir";

	private static final String METADATA_DEFAULT_SCRIPT_GROUP_JSON = "{\"type\":\"SCRIPT_GROUP\",\"name\":\"default_group\",\"title\":\"Default Group\"}";

	private static final String METADATA_FILE_NAME = "meta";

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

		initRepository();
	}

	private void initRepository() throws ConfigurationException {
		File scriptsDefaultGroupDir = new File(rootDir + File.separator
				+ ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE.replace('.', File.separatorChar));

		if (!scriptsDefaultGroupDir.exists()) {
			try {
				FileUtils.forceMkdir(scriptsDefaultGroupDir);

				File metadaFile = new File(scriptsDefaultGroupDir.getPath() + File.separator + METADATA_FILE_NAME);
				FileUtils.writeStringToFile(metadaFile, METADATA_DEFAULT_SCRIPT_GROUP_JSON);
			} catch (IOException e) {
				throw new ConfigurationException(String.format(
						"The repository initialization failure. The error occurred while creating the directory for the default script group [%s]",
						scriptsDefaultGroupDir.getAbsolutePath()), e);
			}
		}
	}

}
