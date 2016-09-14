package com.ilsid.bfa.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Assert;

import com.ilsid.bfa.TestConstants;

public class IOHelper {

	private static final TypeReference<LinkedHashMap<String, String>> MAP_TYPE_REF = new TypeReference<LinkedHashMap<String, String>>() {
	};

	private static final String SCRIPTS_DIR = TestConstants.TEST_RESOURCES_DIR + "/dynamicCode/";

	private static final String BYTECODE_DIR = TestConstants.TEST_RESOURCES_DIR + "/byteCode/";

	public static String loadScript(String fileName) throws Exception {
		String result;
		try (InputStream is = new FileInputStream(new File(SCRIPTS_DIR + fileName));) {
			result = IOUtils.toString(is);
		}

		return result;
	}

	public static String loadFileContents(String dir, String fileName) throws Exception {
		String result;
		try (InputStream is = new FileInputStream(new File(dir + "/" + fileName));) {
			result = IOUtils.toString(is);
		}

		return result;
	}

	public static byte[] loadClass(String fileName) throws Exception {
		byte[] result;

		try (InputStream is = new FileInputStream(BYTECODE_DIR + fileName)) {
			result = IOUtils.toByteArray(is);
		}

		return result;
	}

	public static byte[] loadClass(String dir, String fileName) throws Exception {
		byte[] result;

		try (InputStream is = new FileInputStream(new File(dir + "/" + fileName))) {
			result = IOUtils.toByteArray(is);
		}

		return result;
	}

	public static void zipDirectory(File sourceDir, File destZipFile) throws Exception {
		Collection<File> files = FileUtils.listFiles(sourceDir, null, true);
		OutputStream archiveStream = new FileOutputStream(destZipFile);
		ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP,
				archiveStream);

		for (File file : files) {
			String entryName = getRelativePath(sourceDir, file);
			ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
			archive.putArchiveEntry(entry);

			BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));

			IOUtils.copy(input, archive);
			input.close();
			archive.closeArchiveEntry();
		}

		archive.finish();
		archiveStream.close();
	}

	public static String getRelativePath(File dir, File file) throws IOException {
		int index = dir.getAbsolutePath().length() + 1;
		String path = file.getCanonicalPath();
		return path.substring(index);
	}

	public static Map<String, String> toMap(File jsonFile) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(jsonFile, MAP_TYPE_REF);
	}

	public static Map<String, String> toMap(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, MAP_TYPE_REF);
	}

	public static void assertEqualDirs(File expectedDir, File actualDir) throws Exception {
		Collection<File> expectedFiles = FileUtils.listFiles(expectedDir, null, true);
		List<String> expectedPaths = getRelativeFilePaths(expectedDir, expectedFiles);
		Collection<File> savedFiles = FileUtils.listFiles(actualDir, null, true);
		List<String> actualPaths = getRelativeFilePaths(actualDir, savedFiles);

		Assert.assertEquals(expectedPaths, actualPaths);
	}

	public static Map<String, String> loadMetadata(File metaFile) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, String> result = new ObjectMapper().readValue(metaFile, Map.class);
		return result;
	}

	public static List<File> unzip(File zipFile, File destDir) throws IOException {
		List<File> result = new LinkedList<>();
		try (ZipArchiveInputStream in = new ZipArchiveInputStream(new FileInputStream(zipFile))) {
			ZipArchiveEntry entry = in.getNextZipEntry();
			while (entry != null) {
				if (entry.isDirectory()) {
					entry = in.getNextZipEntry();
					continue;
				}
				File curfile = new File(destDir, entry.getName());
				File parent = curfile.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
				}
				OutputStream out = new FileOutputStream(curfile);
				IOUtils.copy(in, out);
				out.close();
				result.add(curfile);
				entry = in.getNextZipEntry();
			}
		}
		
		return result;
	}

	private static List<String> getRelativeFilePaths(File dir, Collection<File> files) throws Exception {
		List<String> result = new LinkedList<>();
		for (File file : files) {
			result.add(IOHelper.getRelativePath(dir, file));
		}

		return result;
	}
}
