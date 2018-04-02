package com.ilsid.bfa.script;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.persistence.ScriptingRepository;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.filesystem.FilesystemScriptingRepository;

public class ScriptingRepositoryInitializer {

	private static File commonLibDir;

	@SuppressWarnings("serial")
	public static ScriptingRepository init() throws Exception {
		ScriptingRepository repository = new FilesystemScriptingRepository();
		commonLibDir = new File(TestConstants.REPOSITORY_ROOT_DIR_PATH);
		
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", TestConstants.CODE_REPOSITORY_DIR);
				put("bfa.persistence.fs.common_lib_dir", TestConstants.REPOSITORY_ROOT_DIR_PATH);
			}
		});

		DynamicClassLoader.setRepository(repository);

		return repository;
	}

	@SuppressWarnings("serial")
	public static ScriptingRepository init(final String commonLibDirPath) throws Exception {
		ScriptingRepository repository = new FilesystemScriptingRepository();
		commonLibDir = new File(commonLibDirPath);
		
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", TestConstants.CODE_REPOSITORY_DIR);
				put("bfa.persistence.fs.common_lib_dir", commonLibDirPath);
			}
		});

		DynamicClassLoader.setRepository(repository);

		return repository;
	}

	public static void cleanup() throws Exception {
		final File versionFile = new File(TestConstants.CODE_REPOSITORY_DIR, ".version");

		if (versionFile.exists()) {
			Files.delete(versionFile.toPath());
		}

		if (commonLibDir.exists()) {
			FileUtils.forceDelete(commonLibDir);
		}
	}

}
