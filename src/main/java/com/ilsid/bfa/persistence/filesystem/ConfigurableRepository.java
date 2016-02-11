package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.RepositoryConfig;

public abstract class ConfigurableRepository implements Configurable {

	private static final String CONFIG_PROP_ROOT_DIR_NAME = "bfa.persistence.fs.root_dir";

	protected String rootDirPath;

	/**
	 * Defines the configuration for the file system repository. Creates default repository directories.
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
		verifyRootDir(config);

		initDefaultGroup(ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE,
				buildMetadata(Metadata.SCRIPT_GROUP_TYPE));
		initDefaultGroup(ClassNameUtil.GENERATED_ENTITIES_DEFAULT_GROUP_PACKAGE,
				buildMetadata(Metadata.ENTITY_GROUP_TYPE));
	}

	private String buildMetadata(String groupType) {
		StringBuilder json = new StringBuilder();
		json.append("{\"").append(Metadata.TYPE).append("\":\"").append(groupType).append("\",\"").append(Metadata.NAME)
				.append("\":\"").append(Metadata.DEFAULT_GROUP_NAME).append("\",\"").append(Metadata.TITLE)
				.append("\":\"").append(Metadata.DEFAULT_GROUP_TITLE).append("\"}");

		return json.toString();
	}

	private void verifyRootDir(Map<String, String> config) throws ConfigurationException {
		rootDirPath = config.get(CONFIG_PROP_ROOT_DIR_NAME);
		if (rootDirPath == null) {
			throw new ConfigurationException("Required [" + CONFIG_PROP_ROOT_DIR_NAME + "] property not found");
		}

		if (!new File(rootDirPath).isDirectory()) {
			throw new ConfigurationException("[" + rootDirPath + "] value defined by [" + CONFIG_PROP_ROOT_DIR_NAME
					+ "] property is not a directory");
		}

	}

	private void initDefaultGroup(String packageName, String metaData) throws ConfigurationException {
		File defaultGroupDir = new File(rootDirPath + File.separator + packageName.replace('.', File.separatorChar));

		if (!defaultGroupDir.exists()) {
			try {
				FileUtils.forceMkdir(defaultGroupDir);

				File metadaFile = new File(
						defaultGroupDir.getPath() + File.separator + ClassNameUtil.METADATA_FILE_NAME);
				FileUtils.writeStringToFile(metadaFile, metaData);
			} catch (IOException e) {
				throw new ConfigurationException(String.format(
						"The repository initialization failure. The error occurred while creating the default group directory [%s]",
						defaultGroupDir.getAbsolutePath()), e);
			}
		}
	}

}
