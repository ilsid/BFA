package com.ilsid.bfa.action.persistence.filesystem;

import java.util.HashMap;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.persistence.ActionClassLoader;
import com.ilsid.bfa.action.persistence.ActionRepository;

public class ActionRepositoryInitializer {

	public static ActionRepository init() throws Exception {
		return createInstance(TestConstants.CODE_REPOSITORY_DIR);
	}

	public static ActionRepository init(final String scriptingRepositoryPath) throws Exception {
		return createInstance(scriptingRepositoryPath);
	}

	@SuppressWarnings("serial")
	private static ActionRepository createInstance(final String scriptingRepositoryPath) throws Exception {
		ActionRepository repository = new FilesystemActionRepository();
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", scriptingRepositoryPath);
			}
		});
		
		ActionClassLoader.setRepository(repository);

		return repository;
	}

}
