package com.ilsid.bfa.action.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.ilsid.bfa.action.persistence.ActionRepository;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.GroupNameUtil;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.filesystem.ConfigurableRepository;

/**
 * The action repository based on the file system.
 * 
 * @author illia.sydorovych
 *
 */
public class FilesystemActionRepository extends ConfigurableRepository implements ActionRepository {

	private static final String ACTION_ROOT_DIR = "action";

	private static final String CONFIG_FILE_NAME = "config.properties";

	private static final String CLASS_NAME_PROP = "action.class";

	private static final String CLASSES_DIR = "classes";

	private static final String LIB_DIR = "lib";

	private ObjectMapper jsonMapper = new ObjectMapper();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#getImplementationClassName(java.lang.String)
	 */
	@Override
	public String getImplementationClassName(String actionName) throws PersistenceException {
		String actionDir = getActionDir(actionName);
		if (!new File(actionDir).isDirectory()) {
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

		String actionDir = getActionDir(actionName);
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
			String json = jsonMapper.writeValueAsString(metaData);
			FileUtils.writeStringToFile(new File(groupDir, ClassNameUtil.METADATA_FILE_NAME), json);
		} catch (IOException e) {
			throw new PersistenceException(
					String.format("Failed to save the meta-data for the action group [%s]", groupName), e);
		}
	}

	private void checkGroupExists(String groupName) throws PersistenceException {
		File groupDir = getGroupDir(groupName);
		if (!groupDir.isDirectory()) {
			throw new PersistenceException(String.format("The action group [%s] does not exist", groupName));
		}
	}

	private File getGroupDir(String groupName) {
		String actionRootDir = new StringBuilder(rootDirPath).append(File.separatorChar).append(ACTION_ROOT_DIR)
				.toString();
		
		return new File(actionRootDir, GroupNameUtil.getDirs(groupName));
	}

	private URL toURL(File file) {
		try {
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException(String.format("Unexpected error when creating URL for [%s]", file), e);
		}
	}

	private String getActionDir(String actionName) {
		GroupNameUtil.NameParts nameParts = GroupNameUtil.splitName(actionName, ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE);
		String groupDir = GroupNameUtil.getDirs(nameParts.getParentName());
		String actionDir = GroupNameUtil.getDirs(nameParts.getChildName());

		StringBuilder result = new StringBuilder(rootDirPath).append(File.separatorChar).append(ACTION_ROOT_DIR)
				.append(File.separatorChar).append(groupDir).append(File.separatorChar).append(actionDir);

		return result.toString();
	}

	private String getActionClassName(String actionDir) throws PersistenceException {
		Properties props = new Properties();
		try (InputStream is = new FileInputStream(new File(actionDir + File.separatorChar + CONFIG_FILE_NAME))) {
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

	private class JarFilesFilter implements FilenameFilter {

		private static final String JAR_FILE_EXTENSION = ".jar";

		public boolean accept(File dir, String name) {
			return name.endsWith(JAR_FILE_EXTENSION);
		}

	}

}
