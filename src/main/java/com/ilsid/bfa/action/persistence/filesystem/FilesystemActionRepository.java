package com.ilsid.bfa.action.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.ilsid.bfa.action.persistence.ActionRepository;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.filesystem.ConfigurableRepository;

public class FilesystemActionRepository extends ConfigurableRepository implements ActionRepository {

	private static final String CLASS_FILE_EXTENSION = ".class";

	private static final String CLASSES_DIR = File.separatorChar + "classes" + File.separatorChar;

	private static final String CONFIG_FILE_NAME = "config.properties";

	private static final String CLASS_NAME_PROP = "action.class";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.action.persistence.ActionRepository#load(java.lang.String)
	 */
	public byte[] load(String actionName) throws PersistenceException {
		String actionDir = rootDir + File.separatorChar
				+ ClassNameUtil.generateSimpleClassName(actionName, ClassNameUtil.BLANK_CODE);

		String className = getActionClassName(actionDir);
		if (className == null) {
			return null;
		}

		File classFile = new File(actionDir + CLASSES_DIR + ClassNameUtil.getDirs(className) + File.separatorChar
				+ ClassNameUtil.getShortClassName(className) + CLASS_FILE_EXTENSION);

		if (!classFile.exists()) {
			return null;
		}

		byte[] result;

		try {
			try (InputStream is = new FileInputStream(classFile);) {
				result = IOUtils.toByteArray(is);
			}
		} catch (IOException e) {
			throw new PersistenceException(String.format("Failed to load the action class [%s]", className), e);
		}

		return result;
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

}
