package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.JsonUtil;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.PersistenceException;

/**
 * Provides meta-data related routines.
 * 
 * @author illia.sydorovych
 *
 */
public class MetadataUtil {

	private static final FileNamesComparator FILE_NAMES_COMPARATOR = new FileNamesComparator();

	private static final String CLASS_METADATA_SUFFIX = '_' + ClassNameUtil.METADATA_FILE_NAME;

	private static final MetadataFilesFilter METADATA_FILES_FILTER = new MetadataFilesFilter();

	public static final String ALL_TYPES_CRITERION = "ALL_TYPES";

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

	/**
	 * Collects meta-data items for sub-directories in the target directory.
	 * 
	 * @param dir
	 *            the target directory
	 * @param typeCriterion
	 *            the type filter. The result will be filtered by {@link Metadata#TYPE} value. Or all items will be
	 *            collected if the criterion is equal to {@link MetadataUtil#ALL_TYPES_CRITERION}
	 * @param result
	 *            the resulting collection
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	public static void collectSubDirMetadatas(File dir, String typeCriterion, List<Map<String, String>> result)
			throws PersistenceException {
		File[] children = dir.listFiles();
		Arrays.sort(children, FILE_NAMES_COMPARATOR);

		for (int i = 0; i < children.length; i++) {
			File metaFile = new File(children[i].getPath(), ClassNameUtil.METADATA_FILE_NAME);
			if (metaFile.exists()) {
				Map<String, String> metaData = MetadataUtil.loadContents(metaFile);
				String type = metaData.get(Metadata.TYPE);
				if ((type != null && typeCriterion == ALL_TYPES_CRITERION) || typeCriterion.equals(type)) {
					result.add(metaData);
				}
			}
		}
	}

	/**
	 * Collects meta-data items for classes in the target directory.
	 * 
	 * @param dir
	 *            the target directory
	 * @param typeCriterion
	 *            the type filter. The result will be filtered by {@link Metadata#TYPE} value. Or all items will be
	 *            collected if the criterion is equal to {@link MetadataUtil#ALL_TYPES_CRITERION}
	 * @param result
	 *            the resulting collection
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	public static void collectClassMetadatas(File dir, String typeCriterion, List<Map<String, String>> result)
			throws PersistenceException {
		File[] metaFiles = dir.listFiles(METADATA_FILES_FILTER);
		Arrays.sort(metaFiles, FILE_NAMES_COMPARATOR);

		for (int i = 0; i < metaFiles.length; i++) {
			Map<String, String> metaData = MetadataUtil.loadContents(metaFiles[i]);
			String type = metaData.get(Metadata.TYPE);
			if ((type != null && typeCriterion == ALL_TYPES_CRITERION) || typeCriterion.equals(type)) {
				result.add(metaData);
			}
		}
	}

	/**
	 * Add {@link Metadata#PARENT} record into each meta-data item.
	 * 
	 * @param metaDatas
	 *            a list of meta-data items
	 * @param parentName
	 *            the record value
	 */
	public static void addParentRecord(List<Map<String, String>> metaDatas, String parentName) {
		for (Map<String, String> metaData : metaDatas) {
			metaData.put(Metadata.PARENT, parentName);
		}
	}

	private static class MetadataFilesFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(CLASS_METADATA_SUFFIX);
		}

	}

	private static class FileNamesComparator implements Comparator<File> {

		@Override
		public int compare(File f1, File f2) {
			return f1.getName().compareTo(f2.getName());
		}

	}

}
