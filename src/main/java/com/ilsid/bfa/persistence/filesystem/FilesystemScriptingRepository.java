package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.IOUtil;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.ScriptingRepository;

/**
 * The scripting repository based on the file system.
 * 
 * @author illia.sydorovych
 *
 */
public class FilesystemScriptingRepository extends FilesystemRepository implements ScriptingRepository {

	private static final String[] CLASS_FILES_FILTER = new String[] { "class" };

	private static final char DOT = '.';

	private static final String SOURCE_FILE_EXTENSION = ".src";

	private static final String DIAGRAM_FILE_EXTENSION = ".dgm";

	private static final String DIAGRAM_FILE_HUMAN_NAME = "diagram";
	
	private static final String SOURCE_FILE_HUMAN_NAME = "source file";
	
	private static final String GENERATED_SOURCE_FILE_PREFIX = "_generated" + SOURCE_FILE_EXTENSION;

	private static final String CLASS_METADATA_SUFFIX = '_' + ClassNameUtil.METADATA_FILE_NAME;

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

		File classFile = new File(classDir,
				ClassNameUtil.getShortClassName(className).concat(ClassNameUtil.CLASS_FILE_EXTENSION));

		if (!classFile.exists()) {
			return null;
		}

		byte[] result = loadClassContents(classFile, className);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#loadClasses(java.lang.String)
	 */
	public List<Entry<String, byte[]>> loadClasses(String packageName) throws PersistenceException {
		List<Entry<String, byte[]>> result = new LinkedList<>();
		File packageDir = getPackageDir(packageName);

		if (packageDir.isDirectory()) {
			Collection<File> classFiles = FileUtils.listFiles(packageDir, CLASS_FILES_FILTER, true);
			for (File classFile : classFiles) {
				String className = extractClassName(classFile, packageName);
				byte[] byteCode = loadClassContents(classFile, className);
				result.add(new SimpleImmutableEntry<String, byte[]>(className, byteCode));
			}
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
		doSave(className, byteCode, sourceCode, null, null);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#save(java.lang.String, byte[], java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void save(String className, byte[] byteCode, String sourceCode, String generatedCode, String diagram)
			throws PersistenceException {
		doSave(className, byteCode, sourceCode, generatedCode, diagram);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#save(java.lang.String, byte[])
	 */
	@Override
	public void save(String className, byte[] byteCode) throws PersistenceException {
		doSave(className, byteCode, null, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#saveMetadata(java.lang.String,
	 * com.ilsid.bfa.persistence.Metadata)
	 */
	@Override
	public boolean saveMetadata(String className, Map<String, String> metaData) throws PersistenceException {
		File classDir = new File(getClassDirectoryPath(className));
		File classFile = new File(getFilePathPrefix(className) + ClassNameUtil.CLASS_FILE_EXTENSION);

		if (classDir.exists() && classFile.exists()) {
			try {
				String json = jsonMapper.writeValueAsString(metaData);
				String shortClassName = ClassNameUtil.getShortClassName(className);
				String metaFileName = new StringBuilder(shortClassName).append(CLASS_METADATA_SUFFIX).toString();
				FileUtils.writeStringToFile(new File(classDir, metaFileName), json);
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
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#savePackageMetadata(java.lang.String, java.util.Map)
	 */
	@Override
	public boolean savePackageMetadata(String packageName, Map<String, String> metaData) throws PersistenceException {
		File packageDir = getPackageDir(packageName);

		if (packageDir.isDirectory()) {
			try {
				String json = jsonMapper.writeValueAsString(metaData);
				FileUtils.writeStringToFile(new File(packageDir, ClassNameUtil.METADATA_FILE_NAME), json);
			} catch (IOException e) {
				throw new PersistenceException(
						String.format("Failed to save the meta-data for the package [%s]", packageName), e);
			}

			return true;
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#savePackage(java.lang.String, java.util.Map)
	 */
	@Override
	public void savePackage(String packageName, Map<String, String> metaData) throws PersistenceException {
		File packageDir = getPackageDir(packageName);

		if (packageDir.exists()) {
			throw new PersistenceException(String.format("The package [%s] already exists", packageName));
		}

		try {
			packageDir.mkdirs();
		} catch (SecurityException e) {
			throw new PersistenceException(
					String.format("Failed to create the package [%s]. Permission denied.", packageName), e);
		}

		try {
			String json = jsonMapper.writeValueAsString(metaData);
			FileUtils.writeStringToFile(new File(packageDir, ClassNameUtil.METADATA_FILE_NAME), json);
		} catch (IOException e) {
			throw new PersistenceException(
					String.format("Failed to save the meta-data for the package [%s]", packageName), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#delete(java.lang.String)
	 */
	@Override
	public int deletePackage(String packageName) throws PersistenceException {
		File packageDir = getPackageDir(packageName);

		if (!packageDir.isDirectory()) {
			return 0;
		}

		int filesCnt = packageDir.list().length;

		try {
			FileUtils.forceDelete(packageDir);
		} catch (IOException e) {
			throw new PersistenceException(
					String.format("Failed to delete the package directory [%s]", packageDir.getPath()), e);
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
		File classFile = new File(filePathPrefix + ClassNameUtil.CLASS_FILE_EXTENSION);
		File sourceFile = new File(filePathPrefix + SOURCE_FILE_EXTENSION);
		File metaFile = new File(filePathPrefix + CLASS_METADATA_SUFFIX);

		int filesCnt = 0;

		if (deleteFileIfExists(classFile)) {
			filesCnt++;
			if (deleteFileIfExists(sourceFile)) {
				filesCnt++;
				if (deleteFileIfExists(metaFile)) {
					filesCnt++;
				}
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
		return loadAssociatedFileContents(className, SOURCE_FILE_EXTENSION, SOURCE_FILE_HUMAN_NAME);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#loadDiagram(java.lang.String)
	 */
	public String loadDiagram(String className) throws PersistenceException {
		return loadAssociatedFileContents(className, DIAGRAM_FILE_EXTENSION, DIAGRAM_FILE_HUMAN_NAME);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#loadMetadataForChildPackages(java.lang.String,
	 * java.lang.String[])
	 */
	@Override
	public List<Map<String, String>> loadMetadataForChildPackages(String packageName, String... types)
			throws PersistenceException {
		List<Map<String, String>> result = new LinkedList<>();
		File packageDir = getPackageDir(packageName);

		if (!packageDir.isDirectory()) {
			return result;
		}

		String[] typesToCollect = defineTypesToCollect(types);

		for (String type : typesToCollect) {
			MetadataUtil.collectSubDirMetadatas(packageDir, type, result);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#loadMetadataForClasses(java.lang.String, java.lang.String[])
	 */
	@Override
	public List<Map<String, String>> loadMetadataForClasses(String packageName, String... types)
			throws PersistenceException {
		List<Map<String, String>> result = new LinkedList<>();
		File packageDir = getPackageDir(packageName);

		if (!packageDir.isDirectory()) {
			return result;
		}

		String[] typesToCollect = defineTypesToCollect(types);

		for (String type : typesToCollect) {
			MetadataUtil.collectClassMetadatas(packageDir, type, result);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#loadMetadataForPackage(java.lang.String)
	 */
	@Override
	public Map<String, String> loadMetadataForPackage(String packageName) throws PersistenceException {
		File packageDir = getPackageDir(packageName);

		if (!packageDir.isDirectory()) {
			return null;
		}

		File metaFile = new File(packageDir, ClassNameUtil.METADATA_FILE_NAME);
		if (metaFile.exists()) {
			return MetadataUtil.loadContents(metaFile);
		} else {
			return null;
		}
	}

	private byte[] loadClassContents(File classFile, String className) throws PersistenceException {
		byte[] result;
		try {
			result = IOUtil.loadContents(classFile);
		} catch (IOException e) {
			throw new PersistenceException(String.format("Failed to load the class [%s]", className), e);
		}

		return result;
	}

	private String extractClassName(File classFile, String packageName) {
		String convertedPath = classFile.getPath().replace(File.separatorChar, DOT);
		int packageIdx = convertedPath.lastIndexOf(packageName);
		int classExtIdx = convertedPath.lastIndexOf(ClassNameUtil.CLASS_FILE_EXTENSION);
		String className = convertedPath.substring(packageIdx, classExtIdx);

		return className;
	}

	private void doSave(String className, byte[] byteCode, String sourceCode, String generatedCode, String diagram)
			throws PersistenceException {
		if (rootDirPath == null) {
			throw new IllegalStateException("Root directory is not set");
		}

		String shortClassName = ClassNameUtil.getShortClassName(className);
		String fileClassName = shortClassName + ClassNameUtil.CLASS_FILE_EXTENSION;
		String fileClassDir = getClassDirectoryPath(className);

		File classFile = new File(fileClassDir, fileClassName);
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
			File sourceFile = new File(fileClassDir, shortClassName + SOURCE_FILE_EXTENSION);
			try {
				FileUtils.writeStringToFile(sourceFile, sourceCode, "UTF-8");
			} catch (IOException e) {
				throw new PersistenceException(String.format(
						"Failed to save a source code for class [%s] in directory %s", className, rootDirPath), e);
			}
		}

		if (generatedCode != null) {
			File generatedSourceFile = new File(fileClassDir, shortClassName + GENERATED_SOURCE_FILE_PREFIX);
			try {
				FileUtils.writeStringToFile(generatedSourceFile, generatedCode, "UTF-8");
			} catch (IOException e) {
				throw new PersistenceException(
						String.format("Failed to save a generated source code for class [%s] in directory %s",
								className, rootDirPath),
						e);
			}
		}
		
		if (diagram != null) {
			File diagramFile = new File(fileClassDir, shortClassName + DIAGRAM_FILE_EXTENSION);
			try {
				FileUtils.writeStringToFile(diagramFile, diagram, "UTF-8");
			} catch (IOException e) {
				throw new PersistenceException(
						String.format("Failed to save a diagram for class [%s] in directory %s",
								className, rootDirPath),
						e);
			}
		}
	}
	
	private String loadAssociatedFileContents(String className, String fileExtension, String fileHumanName)
			throws PersistenceException {

		final String classDirPath = getClassDirectoryPath(className);
		File classDir = new File(classDirPath);

		if (!classDir.isDirectory()) {
			return null;
		}

		File associatedFile = new File(
				classDirPath + File.separator + ClassNameUtil.getShortClassName(className) + fileExtension);

		if (!associatedFile.exists()) {
			return null;
		}

		String sourceCode;
		try (InputStream is = new FileInputStream(associatedFile)) {
			sourceCode = IOUtils.toString(is);
		} catch (IOException e) {
			throw new PersistenceException(String.format("Failed to load the %s [%s]", fileHumanName, associatedFile.getPath()),
					e);
		}

		return sourceCode;
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

	private File getPackageDir(String packageName) {
		return new File(rootDirPath, packageName.replace(DOT, File.separatorChar));
	}

	private String[] defineTypesToCollect(String[] types) {
		return types.length > 0 ? types : new String[] { MetadataUtil.ALL_TYPES_CRITERION };
	}

}
