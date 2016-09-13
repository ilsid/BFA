package com.ilsid.bfa.persistence.cassandra;

import com.ilsid.bfa.runtime.persistence.RuntimeRepository;
import com.ilsid.bfa.runtime.persistence.cassandra.CassandraRuntimeRepository;

public class CassandraRuntimeRepositoryInitializer {

	public static RuntimeRepository init() throws Exception {
		RuntimeRepository repository = new CassandraRuntimeRepository();
		repository.setConfiguration(CassandraEmbeddedServer.getClientConfig());

		return repository;
	}

}
