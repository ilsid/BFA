package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;

import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.ilsid.bfa.persistence.TransactionManager;

/**
 * The code repository based on the file system.
 * 
 * @author illia.sydorovych
 *
 */
public class FSCodeRepository implements CodeRepository {

	private static final char DOT = '.';

	private static final String ROOT_DIR_PROP_NAME = "bfa.persistence.fs.root_dir";

	private static final String CLASS_FILE_EXTENSION = ".class";

	private static final String SOURCE_FILE_EXTENSION = ".src";

	private String rootDir;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.CodeRepository#load(java.lang.String)
	 */
	@Override
	public byte[] load(String className) throws PersistenceException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.CodeRepository#save(java.lang.String, byte[], java.lang.String)
	 */
	@Override
	public void save(String className, byte[] byteCode, String sourceCode) throws PersistenceException {
		doSave(className, byteCode, sourceCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.CodeRepository#save(java.lang.String, byte[])
	 */
	@Override
	public void save(String className, byte[] byteCode) throws PersistenceException {
		doSave(className, byteCode, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.CodeRepository#update(java.lang.String, byte[], java.lang.String)
	 */
	@Override
	public void update(String className, byte[] byteCode, String sourceCode) throws PersistenceException {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.CodeRepository#delete(java.lang.String)
	 */
	@Override
	public int deletePackage(String packageName) throws PersistenceException {
		final String dirPath = rootDir + File.separatorChar + packageName.replace(DOT, File.separatorChar);
		File packageDir = new File(dirPath);

		if (!packageDir.isDirectory()) {
			return 0;
		}

		int filesCnt = packageDir.list().length;

		try {
			FileUtils.forceDelete(packageDir);
		} catch (IOException e) {
			throw new PersistenceException(String.format("Failed to delete the package directory [%s]", dirPath), e);
		}

		return filesCnt;
	}

	/**
	 * Returns {@link FSTransactionManager} instance.
	 * 
	 * @return {@link FSTransactionManager} instance
	 */
	@Override
	public TransactionManager getTransactionManager() {
		return FSTransactionManager.getInstance();
	}

	private void doSave(String className, byte[] byteCode, String sourceCode) throws PersistenceException {
		if (rootDir == null) {
			throw new IllegalStateException("Root directory is not set");
		}

		String shortClassName = ClassNameUtil.getShortClassName(className);
		String fileClassName = shortClassName + CLASS_FILE_EXTENSION;
		String fileClassDirs = rootDir + File.separatorChar + ClassNameUtil.getDirs(className);

		File classFile = new File(fileClassDirs + File.separatorChar + fileClassName);
		try {
			if (classFile.exists()) {
				throw new PersistenceException(
						String.format("Class [%s] already exists in directory %s", className, rootDir));
			}

			File dirs = new File(fileClassDirs);
			if (!dirs.exists()) {
				dirs.mkdirs();
			}
		} catch (SecurityException e) {
			throw new PersistenceException(String
					.format("Failed to save class [%s]. Permission denied. Root directory: %s", className, rootDir), e);
		}

		try {
			FileUtils.writeByteArrayToFile(classFile, byteCode);
		} catch (IOException e) {
			throw new PersistenceException(
					String.format("Failed to save class [%s] in directory %s", className, rootDir), e);
		}

		if (sourceCode != null) {
			String fileSourceName = shortClassName + SOURCE_FILE_EXTENSION;
			File sourceFile = new File(fileClassDirs + File.separatorChar + fileSourceName);
			try {
				FileUtils.writeStringToFile(sourceFile, sourceCode, "UTF-8");
			} catch (IOException e) {
				throw new PersistenceException(String
						.format("Failed to save a source code for class [%s] in directory %s", className, rootDir), e);
			}
		}
	}

	@Inject
	public void setConfiguration(@RepositoryConfig Map<String, String> config) throws ConfigurationException {
		rootDir = config.get(ROOT_DIR_PROP_NAME);
		if (rootDir == null) {
			throw new ConfigurationException("Required [" + ROOT_DIR_PROP_NAME + "] property not found");
		}

		if (!new File(rootDir).isDirectory()) {
			throw new ConfigurationException(
					"[" + rootDir + "] value defined by [" + ROOT_DIR_PROP_NAME + "] property is not a directory");
		}
	}

}
