package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.ilsid.bfa.common.JsonUtil;
import com.ilsid.bfa.persistence.PersistenceException;

/**
 * Provides meta-data related routines.
 * 
 * @author illia.sydorovych
 *
 */
public class MetadataUtil {

	/**
	 * Loads meta-data file.
	 * 
	 * @param metaFile
	 *            file to load
	 * @return {@link Map} representation of the loaded meta-data
	 * @throws PersistenceException
	 *             if the file can't be read or converted to {@link Map}
	 */
	public static Map<String, String> loadContents(File metaFile) throws PersistenceException {
		Map<String, String> result;
		try {
			result = JsonUtil.toMap(metaFile);
		} catch (IOException e) {
			throw new PersistenceException(String.format("Failed to read meta-data file [%s]", metaFile), e);
		}
		return result;
	}

}
