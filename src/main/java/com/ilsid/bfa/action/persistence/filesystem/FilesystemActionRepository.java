package com.ilsid.bfa.action.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.action.persistence.ActionInfo;
import com.ilsid.bfa.action.persistence.ActionRepository;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.GroupNameUtil;
import com.ilsid.bfa.common.IOUtil;
import com.ilsid.bfa.common.JsonUtil;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.ilsid.bfa.persistence.filesystem.ConfigurableRepository;
import com.ilsid.bfa.persistence.filesystem.MetadataUtil;

/**
 * The action repository based on the file system.
 * 
 * @author illia.sydorovych
 *
 */
public class FilesystemActionRepository extends ConfigurableRepository implements ActionRepository {

	private static final String ACTION_ROOT_DIR = "action";

	private static final String PACKAGE_FILE_EXTENSION = ".zip";

	private static final String DEFAULT_GROUP_DIR = ACTION_ROOT_DIR + File.separatorChar
			+ ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE;

	private static final String CONFIG_FILE_NAME = "config.properties";

	private static final String CLASS_NAME_PROP = "action.class";

	private static final String CLASSES_DIR = "classes";

	private static final String LIB_DIR = "lib";

	private File actionRootDir;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#getImplementationClassName(java.lang.String)
	 */
	@Override
	public String getImplementationClassName(String actionName) throws PersistenceException {
		File actionDir = getActionDir(actionName);
		if (!actionDir.isDirectory()) {
			return null;
		}
		String className = getActionClassName(actionDir);
		return className;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#getActionDependencies(java.lang.String)
	 */
	@Override
	public List<URL> getDependencies(String actionName) throws PersistenceException {
		List<URL> urls = new LinkedList<>();

		File actionDir = getActionDir(actionName);
		File classesDir = new File(actionDir, CLASSES_DIR);
		if (classesDir.isDirectory()) {
			urls.add(toURL(classesDir));
		}

		File libDir = new File(actionDir, LIB_DIR);
		if (libDir.isDirectory()) {
			for (File jarFile : libDir.listFiles(new JarFilesFilter())) {
				urls.add(toURL(jarFile));
			}
		}

		return urls;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#createGroup(java.lang.String)
	 */
	@Override
	public void createGroup(String groupName, Map<String, String> metaData) throws PersistenceException {
		GroupNameUtil.NameParts parts = GroupNameUtil.splitName(groupName);
		final String parentGroupName = parts.getParentName();

		if (parentGroupName != null) {
			checkGroupExists(parentGroupName);
		}

		createNewGroup(groupName, metaData);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#loadGroupMetadata(java.lang.String)
	 */
	@Override
	public Map<String, String> loadMetadataForGroup(String groupName) throws PersistenceException {
		File groupDir = getGroupDir(groupName);

		if (!groupDir.isDirectory()) {
			return null;
		}

		return loadMetaFile(groupDir);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#loadMetadataForTopLevelGroups()
	 */
	@Override
	public List<Map<String, String>> loadMetadataForTopLevelGroups() throws PersistenceException {
		List<Map<String, String>> result = new LinkedList<>();
		MetadataUtil.collectSubDirMetadatas(getRootDir(), Metadata.ACTION_GROUP_TYPE, result);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#loadMetadataForChildGroups(java.lang.String)
	 */
	@Override
	public List<Map<String, String>> loadMetadataForChildGroups(String groupName) throws PersistenceException {
		return collectMetadata(groupName, Metadata.ACTION_GROUP_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#loadMetadataForActions(java.lang.String)
	 */
	@Override
	public List<Map<String, String>> loadMetadataForActions(String groupName) throws PersistenceException {
		return collectMetadata(groupName, Metadata.ACTION_TYPE);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#save(java.lang.String, java.io.InputStream)
	 */
	@Override
	public void save(String actionName, InputStream actionPackage) throws PersistenceException {
		GroupNameUtil.NameParts nameParts = GroupNameUtil.splitName(actionName);
		final String groupName = nameParts.getParentName();

		if (groupName != null) {
			checkGroupExists(groupName);
		}

		if (actionExists(actionName)) {
			throw new PersistenceException(String.format("Action [%s] already exists in the repository", actionName));
		}

		doSave(actionName, actionPackage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#loadInfo(java.lang.String)
	 */
	public ActionInfo loadInfo(String actionName) throws PersistenceException {
		File actionDir = getActionDir(actionName);

		if (!actionDir.isDirectory()) {
			return null;
		}

		String implClassName = getActionClassName(actionDir);

		List<String> dependencies;
		File libDir = new File(actionDir, LIB_DIR);
		if (libDir.isDirectory()) {
			dependencies = Arrays.asList(libDir.list(new JarFilesFilter()));
		} else {
			dependencies = new LinkedList<>();
		}

		return new ActionInfo(implClassName, dependencies);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#delete(java.lang.String)
	 */
	public boolean delete(String actionName) throws PersistenceException {
		if (!actionExists(actionName)) {
			return false;
		}

		File actionDir = getActionDir(actionName);
		try {
			FileUtils.forceDelete(actionDir);
		} catch (IOException e) {
			throw new PersistenceException(String.format("Failed to delete the action [%s]", actionName), e);
		}

		return true;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.filesystem.ConfigurableRepository#setConfiguration(java.util.Map)
	 */
	@Override
	@Inject
	public void setConfiguration(@RepositoryConfig Map<String, String> config) throws ConfigurationException {
		super.setConfiguration(config);
		initDefaultGroup(DEFAULT_GROUP_DIR, buildMetadata(Metadata.ACTION_GROUP_TYPE));
	}

	private Map<String, String> loadMetaFile(File groupDir) throws PersistenceException {
		File metaFile = new File(groupDir, ClassNameUtil.METADATA_FILE_NAME);
		if (metaFile.exists()) {
			return MetadataUtil.loadContents(metaFile);
		} else {
			return null;
		}
	}

	private void createNewGroup(String groupName, Map<String, String> metaData) throws PersistenceException {
		File groupDir = getGroupDir(groupName);
		if (groupDir.isDirectory()) {
			throw new PersistenceException(String.format("The action group [%s] already exists", groupName));
		}

		try {
			groupDir.mkdirs();
		} catch (SecurityException e) {
			throw new PersistenceException(
					String.format("Failed to create the action group [%s]. Permission denied.", groupName), e);
		}

		saveMetadata(groupName, groupDir, metaData);
	}

	private void saveMetadata(String groupName, File groupDir, Map<String, String> metaData)
			throws PersistenceException {
		try {
			String json = JsonUtil.toJsonString(metaData);
			FileUtils.writeStringToFile(new File(groupDir, ClassNameUtil.METADATA_FILE_NAME), json);
		} catch (IOException e) {
			throw new PersistenceException(
					String.format("Failed to save the meta-data for the action group [%s]", groupName), e);
		}
	}

	private void checkGroupExists(String groupName) throws PersistenceException {
		File groupDir = getGroupDir(groupName);
		if (!isGroupDir(groupDir)) {
			throw new PersistenceException(String.format("The action group [%s] does not exist", groupName));
		}
	}

	private boolean isGroupDir(File dir) throws PersistenceException {
		return dir.isDirectory() && isValidGroup(dir);
	}

	private boolean isValidGroup(File groupDir) throws PersistenceException {
		Map<String, String> meta = loadMetaFile(groupDir);

		if (meta != null && Metadata.ACTION_GROUP_TYPE.equals(meta.get(Metadata.TYPE))) {
			return true;
		}

		return false;
	}

	private File getRootDir() {
		if (actionRootDir == null) {
			String path = new StringBuilder(rootDirPath).append(File.separatorChar).append(ACTION_ROOT_DIR).toString();
			actionRootDir = new File(path);
		}

		return actionRootDir;
	}

	private File getGroupDir(String groupName) {
		return new File(getRootDir(), GroupNameUtil.getDirs(groupName));
	}

	private URL toURL(File file) {
		try {
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException(String.format("Unexpected error when creating URL for [%s]", file), e);
		}
	}

	private String getActionDirPath(String actionName) {
		GroupNameUtil.NameParts nameParts = GroupNameUtil.splitName(actionName, ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE);
		String groupDir = GroupNameUtil.getDirs(nameParts.getParentName());
		String actionDir = GroupNameUtil.getDirs(nameParts.getChildName());

		StringBuilder path = new StringBuilder(rootDirPath).append(File.separatorChar).append(ACTION_ROOT_DIR)
				.append(File.separatorChar).append(groupDir).append(File.separatorChar).append(actionDir);

		return path.toString();
	}

	private File getActionDir(String actionName) {
		return new File(getActionDirPath(actionName));
	}

	private String getActionClassName(File actionDir) throws PersistenceException {
		Properties props = new Properties();
		try (InputStream is = new FileInputStream(new File(actionDir, CONFIG_FILE_NAME))) {
			props.load(is);
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			throw new PersistenceException("Failed to load the action configuration", e);
		}

		String className = props.getProperty(CLASS_NAME_PROP);
		if (className == null) {
			return null;
		}

		return className;
	}

	private boolean actionExists(String actionName) throws PersistenceException {
		File dir = getActionDir(actionName);

		if (!dir.isDirectory()) {
			return false;
		}

		String className = getActionClassName(dir);

		return className != null;
	}

	private void doSave(String actionName, InputStream actionPackage) throws PersistenceException {
		final String actionDirPath = getActionDirPath(actionName);
		final File tmpPackageFile = new File(actionDirPath.concat(PACKAGE_FILE_EXTENSION));
		final File actionDir = new File(actionDirPath);

		try (OutputStream out = new FileOutputStream(tmpPackageFile)) {
			IOUtils.copy(actionPackage, out);
			IOUtil.unzip(tmpPackageFile, actionDir);
		} catch (IOException e) {
			throw new PersistenceException(String.format("Failed to save the action [%s]", actionName), e);
		} finally {
			FileUtils.deleteQuietly(tmpPackageFile);
		}

		validatePackageFormat(actionDir);
		saveActionMetadata(actionName, actionDir);
	}

	private void validatePackageFormat(File actionDir) throws PersistenceException {
		final String implClassName = getActionClassName(actionDir);
		if (implClassName == null) {
			throw new PersistenceException("Invalid action package. Configuration is missed");
		}

		final File classesDir = new File(actionDir, CLASSES_DIR);
		File implClassFile = new File(classesDir, ClassNameUtil.getPath(implClassName));
		if (!implClassFile.exists()) {
			throw new PersistenceException(
					String.format("Invalid action package. The implementation class [%s] is missed", implClassName));
		}
	}

	private void saveActionMetadata(String actionName, File actionDir) throws PersistenceException {
		Map<String, String> metaData = createActionMetadata(actionName);
		try {
			String json = JsonUtil.toJsonString(metaData);
			FileUtils.writeStringToFile(new File(actionDir, ClassNameUtil.METADATA_FILE_NAME), json);
		} catch (IOException e) {
			throw new PersistenceException(
					String.format("Failed to save the meta-data for the action [%s]", actionName), e);
		}
	}

	private Map<String, String> createActionMetadata(String actionName) {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put(Metadata.TYPE, Metadata.ACTION_TYPE);
		metaData.put(Metadata.NAME, actionName);
		metaData.put(Metadata.TITLE, GroupNameUtil.splitName(actionName).getChildName());

		return metaData;
	}

	private List<Map<String, String>> collectMetadata(String groupName, String metadataType)
			throws PersistenceException {
		List<Map<String, String>> result = new LinkedList<>();
		File groupDir = getGroupDir(groupName);
		if (isGroupDir(groupDir)) {
			MetadataUtil.collectSubDirMetadatas(groupDir, metadataType, result);
		}

		return result;
	}

	private class JarFilesFilter implements FilenameFilter {

		private static final String JAR_FILE_EXTENSION = ".jar";

		public boolean accept(File dir, String name) {
			return name.endsWith(JAR_FILE_EXTENSION);
		}

	}

}
