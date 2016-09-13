package com.ilsid.bfa.runtime.persistence.cassandra;

import java.util.Date;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.cassandra.CassandraRepository;
import com.ilsid.bfa.persistence.cassandra.CassandraUtil;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

public class CassandraRuntimeRepository extends CassandraRepository implements RuntimeRepository {

	private static final String RUNTIME_ID_ALIAS = "runtime_id";

	private static final String NEXT_RUNTIME_ID_QUERY = "SELECT uuid() AS " + RUNTIME_ID_ALIAS + " FROM system.local";

	private static final String RUNNING_FLOWS_INSERT_STMT = "INSERT INTO running_flows (runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack) VALUES (?, ?, ?, ?, ?, ?, ?)";

	private static final String COMPLETED_FLOWS_INSERT_STMT_TPLT = "INSERT INTO %s (runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack, status, end_time, error_details) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String COMPLETED_FLOWS_BY_DATE_TABLE_NAME = "completed_flows_by_date";

	private static final String COMPLETED_FLOWS_BY_STATUS_TABLE_NAME = "completed_flows_by_status";

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

		PreparedStatement insertByDatePrepStmt = session
				.prepare(String.format(COMPLETED_FLOWS_INSERT_STMT_TPLT, COMPLETED_FLOWS_BY_DATE_TABLE_NAME));
		BoundStatement insertByDateBoundStmt = bindCompletedFlowsStatement(insertByDatePrepStmt, record);

		PreparedStatement insertByStatusPrepStmt = session
				.prepare(String.format(COMPLETED_FLOWS_INSERT_STMT_TPLT, COMPLETED_FLOWS_BY_STATUS_TABLE_NAME));
		BoundStatement insertByStatusBoundStmt = bindCompletedFlowsStatement(insertByStatusPrepStmt, record);

		BatchStatement batch = new BatchStatement();
		batch.add(insertByDateBoundStmt);
		batch.add(insertByStatusBoundStmt);

		try {
			session.execute(batch);
		} catch (RuntimeException e) {
			throw new PersistenceException("Failed to update flow runtime record", e);
		}
	}

	private BoundStatement bindCompletedFlowsStatement(PreparedStatement stmt, ScriptRuntimeDTO record) {
		final Date startTime = record.getStartTime();

		return stmt.bind(record.getRuntimeId(), record.getUserName(), record.getScriptName(), record.getParameters(),
				CassandraUtil.timestampToDateToken(startTime), startTime, record.getCallStack(),
				record.getStatus().getValue(), record.getEndTime(), CassandraUtil.getErrorDetails(record.getError()));
	}

}
