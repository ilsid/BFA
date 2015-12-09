package com.ilsid.bfa.script;

import java.util.HashMap;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.persistence.ScriptingRepository;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.filesystem.FilesystemScriptingRepository;

public class CodeRepositoryInitializer {
	
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


}
