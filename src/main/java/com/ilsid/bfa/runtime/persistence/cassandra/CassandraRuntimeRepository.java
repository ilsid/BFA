package com.ilsid.bfa.runtime.persistence.cassandra;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.QueryPagingOptions;
import com.ilsid.bfa.persistence.cassandra.CassandraRepository;
import com.ilsid.bfa.persistence.cassandra.CassandraUtil;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

public class CassandraRuntimeRepository extends CassandraRepository implements RuntimeRepository {

	private static final String RUNTIME_ID_ALIAS = "runtime_id";

	private static final String NEXT_RUNTIME_ID_QUERY = "SELECT uuid() AS " + RUNTIME_ID_ALIAS + " FROM system.local";

	private static final String RUNNING_FLOWS_INSERT_STMT = "INSERT INTO running_flows (runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack, completed) VALUES (?, ?, ?, ?, ?, ?, ?, false)";
	
	private static final String RUNNING_FLOWS_UPDATE_STMT = "UPDATE running_flows SET completed=true WHERE start_date=? AND runtime_id=? AND start_time=?";

	private static final String COMPLETED_FLOWS_INSERT_STMT_TPLT = "INSERT INTO completed_flows (runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack, end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String FAILED_FLOWS_INSERT_STMT_TPLT = "INSERT INTO failed_flows (runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack, end_time, error_details) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#getNextRuntimeId()
	 */
	@Override
	public Object getNextRuntimeId() throws PersistenceException {
		final Session session = getSession();
		PreparedStatement st = session.prepare(NEXT_RUNTIME_ID_QUERY);
		ResultSet rs = session.execute(st.bind());

		return rs.one().getUUID(RUNTIME_ID_ALIAS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#createRuntimeRecord(com.ilsid.bfa.runtime.dto.
	 * ScriptRuntimeDTO)
	 */
	@Override
	public void createRuntimeRecord(ScriptRuntimeDTO record) throws PersistenceException, IllegalArgumentException {
		final Session session = getSession();
		PreparedStatement prepStmt = session.prepare(RUNNING_FLOWS_INSERT_STMT);
		final Date startTime = record.getStartTime();

		BoundStatement boundStmt = prepStmt.bind(record.getRuntimeId(), record.getUserName(), record.getScriptName(),
				record.getParameters(), CassandraUtil.timestampToDateToken(startTime), startTime,
				record.getCallStack());

		try {
			session.execute(boundStmt);
		} catch (RuntimeException e) {
			throw new PersistenceException("Failed to create flow runtime record", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#updateRuntimeRecord(com.ilsid.bfa.runtime.dto.
	 * ScriptRuntimeDTO)
	 */
	@Override
	public void updateRuntimeRecord(ScriptRuntimeDTO record) throws PersistenceException, IllegalArgumentException {
		final Session session = getSession();

		PreparedStatement insertStmt;
		final RuntimeStatusType runtimeStatus = record.getStatus();

		if (runtimeStatus == RuntimeStatusType.COMPLETED) {
			insertStmt = session.prepare(COMPLETED_FLOWS_INSERT_STMT_TPLT);
		} else if (runtimeStatus == RuntimeStatusType.FAILED) {
			insertStmt = session.prepare(FAILED_FLOWS_INSERT_STMT_TPLT);
		} else {
			throw new IllegalArgumentException("Illegal flow runtime status: " + runtimeStatus);
		}

		BoundStatement insBoundStmt = new BoundStatement(insertStmt);
		final Date startTime = record.getStartTime();
		final UUID runtimeId = (UUID) record.getRuntimeId();
		final String startDate = CassandraUtil.timestampToDateToken(startTime);
		final Collection<String> callStack = record.getCallStack();

		insBoundStmt.setUUID(0, runtimeId);
		insBoundStmt.setString(1, record.getUserName());
		insBoundStmt.setString(2, record.getScriptName());
		insBoundStmt.setList(3, record.getParameters());
		insBoundStmt.setString(4, startDate);
		insBoundStmt.setTimestamp(5, startTime);
		if (callStack == null) {
			insBoundStmt.setList(6, null);
		} else {
			insBoundStmt.setList(6, new LinkedList<String>(callStack));
		}
		insBoundStmt.setTimestamp(7, record.getEndTime());
		if (runtimeStatus == RuntimeStatusType.FAILED) {
			insBoundStmt.setList(8, CassandraUtil.getErrorDetails(record.getError()));
		}
		
		PreparedStatement updateStmt = session.prepare(RUNNING_FLOWS_UPDATE_STMT);
		BoundStatement updBoundStmt = updateStmt.bind(startDate, runtimeId, startTime);
		
		BatchStatement batch = new BatchStatement();
		batch.add(insBoundStmt);
		batch.add(updBoundStmt);
		
		try {
			session.execute(batch);
		} catch (RuntimeException e) {
			throw new PersistenceException("Failed to update flow runtime record", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#fetch(com.ilsid.bfa.runtime.dto. ScriptRuntimeCriteria,
	 * com.ilsid.bfa.persistence.QueryPagingOptions)
	 */
	@Override
	public Collection<ScriptRuntimeDTO> fetch(ScriptRuntimeCriteria criteria, QueryPagingOptions pagingOptions)
			throws PersistenceException {

		return null;
	}

}
