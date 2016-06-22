package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.PersistenceLogger;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.ilsid.bfa.persistence.TransactionManager;
import com.ilsid.bfa.persistence.Transactional;

/**
 * File system based repository.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class FilesystemRepository implements Configurable, Transactional {

	private static final String INIT_VERSION_VALUE = "1";

	private static final String CONFIG_PROP_ROOT_DIR_NAME = "bfa.persistence.fs.root_dir";

	private static final String CONFIG_PROP_LISTEN_UPDATES_NAME = "bfa.persistence.fs.listen_updates";

	private static final String REPOSITORY_LOCKED_ERR_MSG = "Repository is locked. The operation should be tried again";

	private static final String LOCK_FAILED_ERR_MSG = "Failed to lock the repository";

	private static final String LOCK_DELETE_FAILED_MSG_TPLT = "Failed to delete repository lock [{}]";

	private static final String LOCK_FILE_NAME = ".lock";

	static final String VERSION_FILE_NAME = ".version";

	private File lockFile;

	private File versionFile;

	private Logger logger;

	private TransactionManager txManager;

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

		initLockFile();
		initVersionFile();

		if (listenUpdates(config)) {
			initUpdateListener();
		}
	}

	/**
	 * Defines the logger implementation.
	 * 
	 * @param loggerImpl
	 *            the logger instance
	 */
	@Inject
	public void setLogger(@PersistenceLogger Logger loggerImpl) {
		logger = loggerImpl;
	}

	/**
	 * Returns {@link FSTransactionManager} instance.
	 * 
	 * @return {@link FSTransactionManager} instance
	 */
	@Override
	public TransactionManager getTransactionManager() {
		if (txManager == null) {
			txManager = new FSTransactionManager(this);
		}

		return txManager;
	}

	protected void initDefaultGroup(String packageName, String metaData) throws ConfigurationException {
		File defaultGroupDir = new File(rootDirPath, packageName.replace('.', File.separatorChar));

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

	protected String buildMetadata(String groupType) {
		StringBuilder json = new StringBuilder();
		json.append("{\"").append(Metadata.TYPE).append("\":\"").append(groupType).append("\",\"").append(Metadata.NAME)
				.append("\":\"").append(Metadata.DEFAULT_GROUP_NAME).append("\",\"").append(Metadata.TITLE)
				.append("\":\"").append(Metadata.DEFAULT_GROUP_TITLE).append("\"}");

		return json.toString();
	}

	/**
	 * Locks the repository.
	 * 
	 * @throws LockException
	 *             if the repository has been already locked
	 */
	void lock() throws LockException {
		try {
			if (!lockFile.createNewFile()) {
				throw new LockException(REPOSITORY_LOCKED_ERR_MSG);
			}
		} catch (IOException e) {
			throw new LockException(LOCK_FAILED_ERR_MSG, e);
		}
	}

	/**
	 * Unlocks the repository if it is locked (otherwise does nothing).
	 * 
	 */
	void unlock() {
		try {
			Files.deleteIfExists(lockFile.toPath());
		} catch (IOException e) {
			logger.error(LOCK_DELETE_FAILED_MSG_TPLT, lockFile, e);
		}
	}

	boolean isLocked() {
		return lockFile.exists();
	}

	void incrementVersion() throws PersistenceException {
		try {
			String version = FileUtils.readFileToString(versionFile);
			long versionValue = Long.parseLong(version);
			FileUtils.writeStringToFile(versionFile, Long.valueOf(versionValue + 1).toString());
		} catch (IOException | NumberFormatException e) {
			throw new PersistenceException("Failed to update repository version file", e);
		}
	}

	Logger getLogger() {
		return logger;
	}

	File getVersionFile() {
		return versionFile;
	}
	
	String readVersion() throws IOException {
		return FileUtils.readFileToString(versionFile);
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

	private void initLockFile() {
		lockFile = new File(rootDirPath, LOCK_FILE_NAME);
		unlock();
	}

	private void initVersionFile() throws ConfigurationException {
		versionFile = new File(rootDirPath, VERSION_FILE_NAME);
		if (!versionFile.exists()) {
			try {
				FileUtils.writeStringToFile(versionFile, INIT_VERSION_VALUE);
			} catch (IOException e) {
				throw new ConfigurationException("Failed to create repository version file", e);
			}
		}
	}

	private boolean listenUpdates(Map<String, String> config) {
		return Boolean.parseBoolean(config.get(CONFIG_PROP_LISTEN_UPDATES_NAME));
	}
	
	private void initUpdateListener() throws ConfigurationException {
		String initVersion;
		try {
			initVersion = readVersion();
		} catch (IOException e) {
			throw new ConfigurationException("Failed to get repository version", e);
		}
		
		RepositoryUpdateListener.start(this, initVersion);
	}

}
