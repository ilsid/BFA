package com.ilsid.bfa.script;

import java.util.HashMap;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.filesystem.FSCodeRepository;

public class CodeRepositoryInitializer {
	
	@SuppressWarnings("serial")
	public static CodeRepository init() throws Exception {
		CodeRepository repository = new FSCodeRepository();
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", TestConstants.TEST_RESOURCES_DIR + "/code_repository");
			}
		});

		DynamicClassLoader.setRepository(repository);
		
		return repository;
	}


}
