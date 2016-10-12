package com.ilsid.bfa.runtime.persistence.orientdb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.QueryPage;
import com.ilsid.bfa.persistence.QueryPagingOptions;
import com.ilsid.bfa.persistence.orientdb.DatabaseCallback;
import com.ilsid.bfa.persistence.orientdb.OrientdbRepository;
import com.ilsid.bfa.persistence.orientdb.VoidDatabaseCallback;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQLResultset;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
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
	public Object getNextRuntimeId() throws PersistenceException {
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
	 * 
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#createRuntimeRecord(com.ilsid.bfa.runtime.dto.
	 * ScriptRuntimeDTO)
	 */
	@Override
	public void createRuntimeRecord(final ScriptRuntimeDTO record) throws PersistenceException {
		executeInTransaction(new VoidDatabaseCallback() {

			public Void doInDatabase(OrientGraph connection) throws PersistenceException {
				ODocument dbRecord = new ODocument("FlowRuntime");
				dbRecord.field("runtimeId", record.getRuntimeId());
				dbRecord.field("scriptName", record.getScriptName());
				dbRecord.field("status", record.getStatus());
				dbRecord.field("startTime", record.getStartTime());
				if (!record.getParameters().isEmpty()) {
					dbRecord.field("parameters", record.getParameters());
				}
				if (record.getCallStack() != null) {
					dbRecord.field("callStack", record.getCallStack());
				}

				dbRecord.save();

				return null;
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#updateRuntimeRecord(com.ilsid.bfa.runtime.dto.
	 * ScriptRuntimeDTO)
	 */
	@Override
	public void updateRuntimeRecord(final ScriptRuntimeDTO record) throws PersistenceException {
		executeInTransaction(new VoidDatabaseCallback() {

			public Void doInDatabase(OrientGraph connection) throws PersistenceException {
				OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
						"SELECT * FROM FlowRuntime WHERE runtimeId=:runtimeId AND scriptName=:scriptName");

				Map<String, Object> params = new HashMap<>();
				params.put("runtimeId", record.getRuntimeId());
				params.put("scriptName", record.getScriptName());

				List<ODocument> result = connection.getRawGraph().command(query).execute(params);
				ODocument dbRecord = result.get(0);

				dbRecord.field("status", record.getStatus());
				dbRecord.field("endTime", record.getEndTime());
				if (record.getErrorDetails() != null) {
					dbRecord.field("errorDetails", record.getErrorDetails());
				}

				dbRecord.save();

				return null;
			}

		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#fetch(com.ilsid.bfa.runtime.dto. ScriptRuntimeCriteria,
	 * com.ilsid.bfa.persistence.QueryPagingOptions)
	 */
	@Override
	public QueryPage<ScriptRuntimeDTO> fetch(ScriptRuntimeCriteria criteria, QueryPagingOptions pagingOptions)
			throws PersistenceException {

		throw new RuntimeException("Not implemented");
	}

}
