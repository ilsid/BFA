package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

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

	private static final String CONFIG_PROP_ROOT_DIR_NAME = "bfa.persistence.fs.root_dir";

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
		final String classDirPath = getClassDirectoryPath(className);
		File classDir = new File(classDirPath);

		if (!classDir.isDirectory()) {
			return null;
		}

		File classFile = new File(
				classDirPath + File.separator + ClassNameUtil.getShortClassName(className) + CLASS_FILE_EXTENSION);

		if (!classFile.exists()) {
			return null;
		}

		byte[] result;

		try {
			try (InputStream is = new FileInputStream(classFile);) {
				result = IOUtils.toByteArray(is);
			}
		} catch (IOException e) {
			throw new PersistenceException(String.format("Failed to load the class [%s]", className), e);
		}

		return result;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.CodeRepository#loadSourceCode(java.lang.String)
	 */
	@Override
	public String loadSourceCode(String className) throws PersistenceException {
		final String classDirPath = getClassDirectoryPath(className);
		File classDir = new File(classDirPath);

		if (!classDir.isDirectory()) {
			return null;
		}

		File sourceFile = new File(
				classDirPath + File.separator + ClassNameUtil.getShortClassName(className) + SOURCE_FILE_EXTENSION);

		if (!sourceFile.exists()) {
			return null;
		}

		String sourceCode;
		try (InputStream is = new FileInputStream(sourceFile)) {
			sourceCode = IOUtils.toString(is);
		} catch (IOException e) {
			throw new PersistenceException(String.format("Failed to load the source file [%s]", sourceFile.getPath()),
					e);
		}

		return sourceCode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.CodeRepository#getNextRuntimeId()
	 */
	@Override
	public long getNextRuntimeId() throws PersistenceException {
		// TODO: implement
		return 1;
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

	private void doSave(String className, byte[] byteCode, String sourceCode) throws PersistenceException {
		if (rootDir == null) {
			throw new IllegalStateException("Root directory is not set");
		}

		String shortClassName = ClassNameUtil.getShortClassName(className);
		String fileClassName = shortClassName + CLASS_FILE_EXTENSION;
		String fileClassDir = getClassDirectoryPath(className);

		File classFile = new File(fileClassDir + File.separatorChar + fileClassName);
		try {
			if (classFile.exists()) {
				throw new PersistenceException(
						String.format("Class [%s] already exists in directory %s", className, rootDir));
			}

			File dirs = new File(fileClassDir);
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
			File sourceFile = new File(fileClassDir + File.separatorChar + fileSourceName);
			try {
				FileUtils.writeStringToFile(sourceFile, sourceCode, "UTF-8");
			} catch (IOException e) {
				throw new PersistenceException(String
						.format("Failed to save a source code for class [%s] in directory %s", className, rootDir), e);
			}
		}
	}

	private String getClassDirectoryPath(String className) {
		return rootDir + File.separatorChar + ClassNameUtil.getDirs(className);
	}

}
