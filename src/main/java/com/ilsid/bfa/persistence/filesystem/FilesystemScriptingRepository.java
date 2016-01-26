package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.ScriptingRepository;
import com.ilsid.bfa.persistence.TransactionManager;

/**
 * The scripting repository based on the file system.
 * 
 * @author illia.sydorovych
 *
 */
public class FilesystemScriptingRepository extends ConfigurableRepository implements ScriptingRepository {

	private static final char DOT = '.';

	private static final String CLASS_FILE_EXTENSION = ".class";

	private static final String SOURCE_FILE_EXTENSION = ".src";

	private static final FileNamesComparator FILE_NAMES_COMPARATOR = new FileNamesComparator();

	private AtomicLong runtimeId = new AtomicLong(System.currentTimeMillis());

	private ObjectMapper jsonMapper = new ObjectMapper();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#load(java.lang.String)
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
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#save(java.lang.String, byte[], java.lang.String)
	 */
	@Override
	public void save(String className, byte[] byteCode, String sourceCode) throws PersistenceException {
		doSave(className, byteCode, sourceCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#save(java.lang.String, byte[])
	 */
	@Override
	public void save(String className, byte[] byteCode) throws PersistenceException {
		doSave(className, byteCode, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#saveMetadata(java.lang.String,
	 * com.ilsid.bfa.persistence.Metadata)
	 */
	public boolean saveMetadata(String className, Map<String, String> metaData) throws PersistenceException {
		File classDir = new File(getClassDirectoryPath(className));
		File classFile = new File(getFilePathPrefix(className) + CLASS_FILE_EXTENSION);

		if (classDir.exists() && classFile.exists()) {
			try {
				String json = jsonMapper.writeValueAsString(metaData);
				FileUtils.writeStringToFile(new File(classDir + File.separator + ClassNameUtil.METADATA_FILE_NAME),
						json);
			} catch (IOException e) {
				throw new PersistenceException(
						String.format("Failed to save the meta-data for the class [%s]", className), e);
			}

			return true;
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#delete(java.lang.String)
	 */
	@Override
	public int deletePackage(String packageName) throws PersistenceException {
		final String dirPath = rootDirPath + File.separatorChar + packageName.replace(DOT, File.separatorChar);
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
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#deleteClass(java.lang.String)
	 */
	@Override
	public int deleteClass(String className) throws PersistenceException {
		String filePathPrefix = getFilePathPrefix(className);
		File classFile = new File(filePathPrefix + CLASS_FILE_EXTENSION);
		File sourceFile = new File(filePathPrefix + SOURCE_FILE_EXTENSION);

		int filesCnt = 0;

		if (deleteFileIfExists(classFile)) {
			filesCnt++;
			if (deleteFileIfExists(sourceFile)) {
				filesCnt++;
			}
		}

		return filesCnt;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#loadSourceCode(java.lang.String)
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
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#getNextRuntimeId()
	 */
	@Override
	public long getNextRuntimeId() throws PersistenceException {
		return runtimeId.incrementAndGet();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#loadGroupMetadatas()
	 */
	public List<Map<String, String>> loadGroupMetadatas() throws PersistenceException {
		List<Map<String, String>> result = new LinkedList<>();
		collectMetadatas(scriptsRootDir, Metadata.SCRIPT_GROUP_TYPE, Metadata.ROOT_PARENT_NAME, result);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#loadSubGroupMetadatas(java.lang.String)
	 */
	public List<Map<String, String>> loadSubGroupMetadatas(String groupName) throws PersistenceException {
		return loadMetadataItems(groupName, Metadata.SCRIPT_GROUP_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#getScripts(java.lang.String)
	 */
	public List<Map<String, String>> loadScriptMetadatas(String groupName) throws PersistenceException {
		return loadMetadataItems(groupName, Metadata.SCRIPT_TYPE);
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
		if (rootDirPath == null) {
			throw new IllegalStateException("Root directory is not set");
		}

		String shortClassName = ClassNameUtil.getShortClassName(className);
		String fileClassName = shortClassName + CLASS_FILE_EXTENSION;
		String fileClassDir = getClassDirectoryPath(className);

		File classFile = new File(fileClassDir + File.separatorChar + fileClassName);
		try {
			if (classFile.exists()) {
				throw new PersistenceException(
						String.format("Class [%s] already exists in directory %s", className, rootDirPath));
			}

			File dirs = new File(fileClassDir);
			if (!dirs.exists()) {
				dirs.mkdirs();
			}
		} catch (SecurityException e) {
			throw new PersistenceException(String.format(
					"Failed to save class [%s]. Permission denied. Root directory: %s", className, rootDirPath), e);
		}

		try {
			FileUtils.writeByteArrayToFile(classFile, byteCode);
		} catch (IOException e) {
			throw new PersistenceException(
					String.format("Failed to save class [%s] in directory %s", className, rootDirPath), e);
		}

		if (sourceCode != null) {
			String fileSourceName = shortClassName + SOURCE_FILE_EXTENSION;
			File sourceFile = new File(fileClassDir + File.separatorChar + fileSourceName);
			try {
				FileUtils.writeStringToFile(sourceFile, sourceCode, "UTF-8");
			} catch (IOException e) {
				throw new PersistenceException(String.format(
						"Failed to save a source code for class [%s] in directory %s", className, rootDirPath), e);
			}
		}
	}

	private String getClassDirectoryPath(String className) {
		return rootDirPath + File.separatorChar + ClassNameUtil.getDirs(className);
	}

	private String getFilePathPrefix(String className) {
		return rootDirPath + File.separatorChar + className.replace(DOT, File.separatorChar);
	}

	private boolean deleteFileIfExists(File file) throws PersistenceException {
		if (file.exists() && file.isFile()) {
			try {
				FileUtils.forceDelete(file);
			} catch (IOException e) {
				throw new PersistenceException(String.format("Failed to delete file [%s]", file.getName()), e);
			}
			return true;
		} else {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> loadContents(File metaFile) throws PersistenceException {
		Map<String, String> result;
		try {
			result = jsonMapper.readValue(metaFile, Map.class);
		} catch (IOException e) {
			throw new PersistenceException("Failed to read meta-data", e);
		}
		return result;
	}

	private void collectMetadatas(File dir, String typeCriteria, String parentName, List<Map<String, String>> result)
			throws PersistenceException {
		File[] children = dir.listFiles();
		Arrays.sort(children, FILE_NAMES_COMPARATOR);

		for (int i = 0; i < children.length; i++) {
			File metaFile = new File(children[i].getPath() + File.separator + ClassNameUtil.METADATA_FILE_NAME);
			if (metaFile.exists()) {
				Map<String, String> metaData = loadContents(metaFile);
				String type = metaData.get(Metadata.TYPE);
				if (typeCriteria.equals(type)) {
					metaData.put(Metadata.PARENT, parentName);
					result.add(metaData);
				}
			}
		}
	}

	private List<Map<String, String>> loadMetadataItems(String groupName, String itemType) throws PersistenceException {
		List<Map<String, String>> result = new LinkedList<>();
		File groupDir = new File(scriptsRootDir.getPath() + File.separator
				+ groupName.replace(ClassNameUtil.GROUP_SEPARATOR, File.separator));

		if (!groupDir.isDirectory()) {
			return result;
		}
		collectMetadatas(groupDir, itemType, groupName, result);

		return result;
	}

	private static class FileNamesComparator implements Comparator<File> {

		public int compare(File f1, File f2) {
			return f1.getName().compareTo(f2.getName());
		}

	}

}
