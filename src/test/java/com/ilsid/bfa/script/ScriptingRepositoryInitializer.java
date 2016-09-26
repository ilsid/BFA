package com.ilsid.bfa.script;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.persistence.ScriptingRepository;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.filesystem.FilesystemScriptingRepository;

public class ScriptingRepositoryInitializer {

	@SuppressWarnings("serial")
	public static ScriptingRepository init() throws Exception {
		ScriptingRepository repository = new FilesystemScriptingRepository();
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", TestConstants.CODE_REPOSITORY_DIR);
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
	}

}
