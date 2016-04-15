package com.ilsid.bfa.script;

import java.util.HashMap;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;
import com.ilsid.bfa.runtime.persistence.orientdb.OrientdbRuntimeRepository;

public class RuntimeRepositoryInitializer {

	@SuppressWarnings("serial")
	public static RuntimeRepository init() throws Exception {
		final RuntimeRepository repository = new OrientdbRuntimeRepository();
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.orientdb.url", TestConstants.DATABASE_URL);
				put("bfa.persistence.orientdb.user", TestConstants.DATABASE_USER);
				put("bfa.persistence.orientdb.password", TestConstants.DATABASE_PASSWORD);
			}
		});

		return repository;
	}

}
