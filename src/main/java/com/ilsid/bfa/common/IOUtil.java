package com.ilsid.bfa.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

/**
 * I/O routines.
 * 
 * @author illia.sydorovych
 *
 */
public class IOUtil {

	/**
	 * Unzips a source file into a destination directory.
	 * 
	 * @param sourceFile
	 *            file to unzip
	 * @param destDir
	 *            destination directory
	 * @throws IOException
	 *             in case of the operation failure
	 */
	public static void unzip(File sourceFile, File destDir) throws IOException {
		try (ZipFile zipFile = new ZipFile(sourceFile)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File entryDest = new File(destDir, entry.getName());
				if (entry.isDirectory()) {
					entryDest.mkdirs();
				} else {
					entryDest.getParentFile().mkdirs();
					InputStream in = zipFile.getInputStream(entry);
					try (OutputStream out = new FileOutputStream(entryDest)) {
						IOUtils.copy(in, out);
						IOUtils.closeQuietly(in);
					}
				}
			}
		}
	}

}
