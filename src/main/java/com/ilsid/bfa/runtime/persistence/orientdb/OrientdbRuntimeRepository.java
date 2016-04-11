package com.ilsid.bfa.runtime.persistence.orientdb;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

/**
 * OrientDB based runtime repository.
 * 
 * @author illia.sydorovych
 *
 */
public class OrientdbRuntimeRepository implements RuntimeRepository {

	private AtomicLong runtimeId = new AtomicLong(System.currentTimeMillis());

	/**
	 * Releases resources.
	 */
	public static void release() {
		//TODO: implement
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#getNextRuntimeId()
	 */
	// TODO: replace stub with actual implementation
	@Override
	public long getNextRuntimeId() throws PersistenceException {
		return runtimeId.incrementAndGet();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.Configurable#setConfiguration(java.util.Map)
	 */
	@Override
	@Inject
	public void setConfiguration(@RepositoryConfig Map<String, String> config) throws ConfigurationException {
		//TODO: implement
	}

}
