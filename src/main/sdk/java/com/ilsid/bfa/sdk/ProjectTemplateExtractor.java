package com.ilsid.bfa.sdk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

class ProjectTemplateExtractor {

	private static final String JAR_NAME = "bfa-sdk.jar";

	private static final String PROJECT_TEMPLATE_DIR = "template";

	public void extract(final String outputDir) throws IOException {
		createOutputDirIfNotExists(outputDir);
		
		Map<String, String> zipProps = new HashMap<>();
		zipProps.put("create", "false");
		zipProps.put("encoding", "UTF-8");
		final File currentDir = new File(Paths.get("").toAbsolutePath().toUri());
		URI zipDisk = URI.create("jar:" + new File(currentDir, JAR_NAME).toURI().toString());
		try (FileSystem zipFS = FileSystems.newFileSystem(zipDisk, zipProps)) {
			Files.walkFileTree(zipFS.getPath(PROJECT_TEMPLATE_DIR), new SimpleFileVisitor<Path>() {
				
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					String destFileName = getDestinationName(file);
					System.out.println("Extracting " + destFileName);
					try {
						try (OutputStream fos = new FileOutputStream(new File(currentDir, destFileName))) {
							Files.copy(zipFS.getPath(file.toString()), fos);
						}
					} catch (IOException e) {
						e.printStackTrace();
						return FileVisitResult.TERMINATE;
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					// Skip creation of the root dir
					if (dir.getNameCount() > 1) {
						String destDirName = getDestinationName(dir);
						System.out.println("Extracting " + destDirName);
						new File(currentDir, destDirName).mkdirs();
					}
					return FileVisitResult.CONTINUE;
				}

				private String getDestinationName(Path path) {
					StringBuilder destName = new StringBuilder();
					if (outputDir != null) {
						destName.append(outputDir);
					}
					for (int i = 1; i < path.getNameCount(); i++) {
						destName.append('/').append(path.getName(i));
					}

					return destName.toString();
				}
			});
		}
	}
	
	private void createOutputDirIfNotExists(String dirPath) {
		if (dirPath == null) {
			return;
		}
		
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
}
