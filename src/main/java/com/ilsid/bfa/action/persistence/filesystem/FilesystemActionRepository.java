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
import java.util.Properties;

import com.ilsid.bfa.action.persistence.ActionRepository;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.filesystem.ConfigurableRepository;

/**
 * The action repository based on the file system.
 * 
 * @author illia.sydorovych
 *
 */
public class FilesystemActionRepository extends ConfigurableRepository implements ActionRepository {

	private static final String DEFAULT_ACTION_ROOT_DIR = File.separatorChar + "action" + File.separatorChar
			+ "default_group" + File.separatorChar;

	private static final String CONFIG_FILE_NAME = "config.properties";

	private static final String CLASS_NAME_PROP = "action.class";

	private static final String CLASSES_DIR = File.separatorChar + "classes" + File.separatorChar;

	private static final String LIB_DIR = File.separatorChar + "lib";

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
		File classesDir = new File(actionDir + CLASSES_DIR);
		if (classesDir.isDirectory()) {
			urls.add(toURL(classesDir));
		}

		File libDir = new File(actionDir + LIB_DIR);
		if (libDir.isDirectory()) {
			for (File jarFile : libDir.listFiles(new JarFilesFilter())) {
				urls.add(toURL(jarFile));
			}
		}

		return urls;
	}

	private URL toURL(File file) {
		try {
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException(String.format("Unexpected error when creating URL for [%s]", file), e);
		}
	}

	private String getActionDir(String actionName) {
		return rootDir + DEFAULT_ACTION_ROOT_DIR
				+ ClassNameUtil.generateSimpleClassName(actionName, ClassNameUtil.BLANK_CODE);
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
