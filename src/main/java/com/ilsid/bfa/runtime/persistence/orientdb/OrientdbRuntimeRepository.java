package com.ilsid.bfa.runtime.persistence.orientdb;

import java.util.concurrent.atomic.AtomicLong;

import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.orientdb.OrientdbRepository;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

/**
 * OrientDB based runtime repository.
 * 
 * @author illia.sydorovych
 *
 */
public class OrientdbRuntimeRepository extends OrientdbRepository implements RuntimeRepository {
	
	private AtomicLong runtimeId = new AtomicLong(System.currentTimeMillis());

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

}
