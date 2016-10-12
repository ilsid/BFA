package com.ilsid.bfa.runtime.persistence.cassandra;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PagingState;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.QueryPage;
import com.ilsid.bfa.persistence.QueryPagingOptions;
import com.ilsid.bfa.persistence.cassandra.CassandraRepository;
import com.ilsid.bfa.persistence.cassandra.CassandraUtil;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

// FIXME: Data model should be updated. "ddMMyyyy" partition key is a bad one for saving/updating of "running flow" and "completed flow" 
// records, as it is going to be a hot spot under a heavy load. Also index for running_flows.completed column is under concern 
// (see https://docs.datastax.com/en/cql/3.3/cql/cql_using/useWhenIndex.html#useWhenIndex__when-no-index)
/**
 * Cassandra based runtime repository.
 * 
 * @author illia.sydorovych
 *
 */
public class CassandraRuntimeRepository extends CassandraRepository implements RuntimeRepository {

	private static final RuntimeRowConverter FAILED_FLOW_CONVERTER = new FailedFlowConverter();

	private static final RuntimeRowConverter RUNNING_FLOW_CONVERTER = new RunningFlowConverter();

	private static final RuntimeRowConverter COMPLETED_FLOW_CONVERTER = new CompletedFlowConverter();

	static final String RUNTIME_ID_ALIAS = "runtime_id";

	static final String NEXT_RUNTIME_ID_QUERY = "SELECT uuid() AS " + RUNTIME_ID_ALIAS + " FROM system.local";

	static final String RUNNING_FLOWS_INSERT_STMT = "INSERT INTO running_flows (runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack, completed) VALUES (?, ?, ?, ?, ?, ?, ?, false)";

	static final String RUNNING_FLOWS_UPDATE_STMT = "UPDATE running_flows SET completed=true WHERE start_date=? AND runtime_id=? AND start_time=?";

	static final String COMPLETED_FLOWS_INSERT_STMT = "INSERT INTO completed_flows (runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack, end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	static final String FAILED_FLOWS_INSERT_STMT = "INSERT INTO failed_flows (runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack, end_time, error_details) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	static final String FAILED_FLOWS_SELECT_STMT = "SELECT runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack, end_time, error_details FROM failed_flows WHERE start_date=?";

	static final String RUNNING_FLOWS_SELECT_STMT = "SELECT runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack FROM running_flows WHERE start_date=? AND completed=false";

	static final String COMPLETED_FLOWS_SELECT_STMT = "SELECT runtime_id, user_name, script_name, parameters, start_date, "
			+ "start_time, call_stack, end_time FROM completed_flows WHERE start_date=?";

	private final Map<String, PreparedStatement> preparedStatements = new HashMap<>();

	@Override
	protected void prepareStatements(Session session) throws PersistenceException {
		String[] statements = { NEXT_RUNTIME_ID_QUERY, RUNNING_FLOWS_INSERT_STMT, RUNNING_FLOWS_UPDATE_STMT,
				COMPLETED_FLOWS_INSERT_STMT, FAILED_FLOWS_INSERT_STMT, FAILED_FLOWS_SELECT_STMT,
				RUNNING_FLOWS_SELECT_STMT, COMPLETED_FLOWS_SELECT_STMT };

		for (String stmt : statements) {
			preparedStatements.put(stmt, session.prepare(stmt));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#getNextRuntimeId()
	 */
	@Override
	public Object getNextRuntimeId() throws PersistenceException {
		final UUID result;
		final Session session = getSession();
		try {
			final PreparedStatement stmt = preparedStatements.get(NEXT_RUNTIME_ID_QUERY);
			ResultSet rs = session.execute(stmt.bind());

			result = rs.one().getUUID(RUNTIME_ID_ALIAS);
		} catch (RuntimeException e) {
			throw new PersistenceException("Failed to obtain flow runtime id", e);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.runtime.persistence.RuntimeRepository#createRuntimeRecord(com.ilsid.bfa.runtime.dto.
	 * ScriptRuntimeDTO)
	 */
	@Override
	public void createRuntimeRecord(ScriptRuntimeDTO record) throws PersistenceException {
		final Session session = getSession();
		try {
			PreparedStatement prepStmt = preparedStatements.get(RUNNING_FLOWS_INSERT_STMT);
			final Date startTime = record.getStartTime();

			BoundStatement boundStmt = prepStmt.bind(record.getRuntimeId(), record.getUserName(),
					record.getScriptName(), record.getParameters(), CassandraUtil.timestampToDateToken(startTime),
					startTime, record.getCallStack());

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
		try {
			Statement insertStmt = createCompletedFlowInsertStatement(session, record);
			Statement updateStmt = createRunningFlowUpdateStatement(session, record);

			BatchStatement batch = new BatchStatement();
			batch.add(insertStmt);
			batch.add(updateStmt);

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
	public QueryPage<ScriptRuntimeDTO> fetch(ScriptRuntimeCriteria criteria, QueryPagingOptions pagingOptions)
			throws PersistenceException {

		RuntimeStatusType status = criteria.getStatus();
		final Session session = getSession();

		QueryPage<ScriptRuntimeDTO> result;
		PreparedStatement stmt;
		RuntimeRowConverter rowConverter;
		try {
			if (status == RuntimeStatusType.FAILED) {
				stmt = preparedStatements.get(FAILED_FLOWS_SELECT_STMT);
				rowConverter = FAILED_FLOW_CONVERTER;
			} else if (status == RuntimeStatusType.INPROGRESS) {
				stmt = preparedStatements.get(RUNNING_FLOWS_SELECT_STMT);
				rowConverter = RUNNING_FLOW_CONVERTER;
			} else if (status == RuntimeStatusType.COMPLETED) {
				stmt = preparedStatements.get(COMPLETED_FLOWS_SELECT_STMT);
				rowConverter = COMPLETED_FLOW_CONVERTER;
			} else {
				throw new IllegalArgumentException("Wrong flow runtime status: " + status);
			}

			BoundStatement boundStmt = stmt.bind(CassandraUtil.timestampToDateToken(criteria.getStartDate()));
			result = getPagedResult(session, boundStmt, rowConverter, pagingOptions);

		} catch (RuntimeException e) {
			throw new PersistenceException("Failed to fetch flow runtime records", e);
		}

		return result;
	}

	private BoundStatement createCompletedFlowInsertStatement(Session session, ScriptRuntimeDTO record) {
		PreparedStatement insertStmt;
		final RuntimeStatusType status = record.getStatus();

		if (status == RuntimeStatusType.COMPLETED) {
			insertStmt = preparedStatements.get(COMPLETED_FLOWS_INSERT_STMT);
		} else if (status == RuntimeStatusType.FAILED) {
			insertStmt = preparedStatements.get(FAILED_FLOWS_INSERT_STMT);
		} else {
			throw new IllegalArgumentException("Illegal flow runtime status: " + status);
		}

		BoundStatement boundStmt = new BoundStatement(insertStmt);
		final Date startTime = record.getStartTime();

		boundStmt.setUUID(0, (UUID) record.getRuntimeId());
		boundStmt.setString(1, record.getUserName());
		boundStmt.setString(2, record.getScriptName());
		boundStmt.setList(3, record.getParameters());
		boundStmt.setString(4, CassandraUtil.timestampToDateToken(startTime));
		boundStmt.setTimestamp(5, startTime);

		final Collection<String> callStack = record.getCallStack();
		if (callStack == null) {
			boundStmt.setList(6, null);
		} else {
			boundStmt.setList(6, new LinkedList<String>(callStack));
		}

		boundStmt.setTimestamp(7, record.getEndTime());

		if (status == RuntimeStatusType.FAILED) {
			boundStmt.setList(8, new LinkedList<String>(record.getErrorDetails()));
		}

		return boundStmt;
	}

	private BoundStatement createRunningFlowUpdateStatement(Session session, ScriptRuntimeDTO record) {
		PreparedStatement updateStmt = preparedStatements.get(RUNNING_FLOWS_UPDATE_STMT);

		final Date startTime = record.getStartTime();
		BoundStatement boundStmt = updateStmt.bind(CassandraUtil.timestampToDateToken(startTime), record.getRuntimeId(),
				startTime);

		return boundStmt;
	}

	private QueryPage<ScriptRuntimeDTO> getPagedResult(Session session, BoundStatement boundStmt,
			RuntimeRowConverter rowConverter, QueryPagingOptions pagingOptions) throws PersistenceException {

		List<ScriptRuntimeDTO> fetchResult = new LinkedList<>();
		String nextPageToken = null;

		boundStmt.setFetchSize(pagingOptions.getResultsPerPage());

		final String pageToken = pagingOptions.getPageToken();
		if (pageToken != null) {
			boundStmt.setPagingState(toPagingState(pageToken));
		}

		ResultSet rs = session.execute(boundStmt);
		int remainingCount = rs.getAvailableWithoutFetching();

		for (Row row : rs) {
			fetchResult.add(rowConverter.toRecord(row));
			if (--remainingCount == 0) {
				break;
			}
		}

		PagingState nextPage = rs.getExecutionInfo().getPagingState();

		if (nextPage != null) {
			nextPageToken = nextPage.toString();
		}

		return new QueryPage<ScriptRuntimeDTO>(fetchResult, nextPageToken);
	}

	private static abstract class RuntimeRowConverter {

		public ScriptRuntimeDTO toRecord(Row row) {
			ScriptRuntimeDTO record = new ScriptRuntimeDTO().setRuntimeId(row.getUUID(0)).setUserName(row.getString(1))
					.setScriptName(row.getString(2)).setParameters(row.getList(3, String.class))
					.setStartTime(row.getTimestamp(5)).setCallStack(row.getList(6, String.class));

			return record;
		};

	}

	private static class FailedFlowConverter extends RuntimeRowConverter {

		@Override
		public ScriptRuntimeDTO toRecord(Row row) {
			return super.toRecord(row).setEndTime(row.getTimestamp(7)).setErrorDetails(row.getList(8, String.class))
					.setStatus(RuntimeStatusType.FAILED);
		}

	}

	private static class RunningFlowConverter extends RuntimeRowConverter {

		@Override
		public ScriptRuntimeDTO toRecord(Row row) {
			return super.toRecord(row).setStatus(RuntimeStatusType.INPROGRESS);
		}

	}

	private static class CompletedFlowConverter extends RuntimeRowConverter {

		@Override
		public ScriptRuntimeDTO toRecord(Row row) {
			return super.toRecord(row).setEndTime(row.getTimestamp(7)).setStatus(RuntimeStatusType.COMPLETED);
		}

	}

}
