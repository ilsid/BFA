package com.ilsid.bfa.runtime.persistence.orientdb;

import java.util.List;

import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.orientdb.DatabaseCallback;
import com.ilsid.bfa.persistence.orientdb.OrientdbRepository;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQLResultset;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

/**
 * OrientDB based runtime repository.
 * 
 * @author illia.sydorovych
 *
 */
public class OrientdbRuntimeRepository extends OrientdbRepository implements RuntimeRepository {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.persistence.ScriptingRepository#getNextRuntimeId()
	 */
	@Override
	public long getNextRuntimeId() throws PersistenceException {
		List<ODocument> resultSet = executeInTransaction(new DatabaseCallback<List<ODocument>>() {

			public List<ODocument> doInDatabase(OrientGraph connection) throws PersistenceException {
				String expr = "UPDATE SequenceProvider INCREMENT value = 1 RETURN AFTER $current WHERE name = 'FlowRuntimeSequence'";
				OCommandSQLResultset cmd = new OCommandSQLResultset(expr);
				List<ODocument> result = connection.getRawGraph().command(cmd).execute();

				return result;
			}
		});

		return resultSet.get(0).field("value");
	}

	/*
	 * (non-Javadoc)
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#createRuntimeRecord(com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO)
	 */
	public void createRuntimeRecord(ScriptRuntimeDTO record) throws PersistenceException {
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#updateRuntimeRecord(com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO)
	 */
	public void updateRuntimeRecord(ScriptRuntimeDTO record) throws PersistenceException {
		// TODO Auto-generated method stub
		
	}

}
