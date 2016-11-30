package com.ilsid.bfa.runtime.persistence.cassandra;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.ilsid.bfa.common.DateHelper;
import com.ilsid.bfa.persistence.cassandra.CassandraEmbeddedServer;

public class RuntimeDatabaseFixture {

	public static final String USER_NAME = "some user";

	private static final LinkedList<String> EMPTY_LIST = new LinkedList<>();

	private static final SimpleDateFormat TOKEN_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	public static void insertFailedFlows(int recordsCount, final Date initTime) {
		insertFailedFlows(recordsCount, initTime, EMPTY_LIST, EMPTY_LIST, EMPTY_LIST);
	}

	public static void insertFailedFlows(int recordsCount, final Date initTime, List<String> parameters,
			List<String> callStack, List<String> errorDetails) {

		final String startDate = TOKEN_DATE_FORMAT.format(initTime);

		for (int cnt = 0; cnt < recordsCount; cnt++) {
			Date startTime = DateHelper.addMinutes(initTime, cnt);
			Date endTime = startTime;

			CassandraEmbeddedServer.getClient().executeBoundStatement(
					CassandraRuntimeRepository.FAILED_FLOWS_INSERT_STMT, UUID.randomUUID(), USER_NAME,
					"Test Script " + (cnt + 1), parameters, startDate, startTime, callStack, endTime, errorDetails);

		}
	}

	public static void insertRunningFlows(int recordsCount, final Date initTime) {
		insertRunningFlows(recordsCount, initTime, EMPTY_LIST, EMPTY_LIST);
	}

	public static void insertRunningFlows(int recordsCount, final Date initTime, List<String> parameters,
			List<String> callStack) {

		final String startDate = TOKEN_DATE_FORMAT.format(initTime);

		for (int cnt = 0; cnt < recordsCount; cnt++) {
			Date startTime = DateHelper.addMinutes(initTime, cnt);

			CassandraEmbeddedServer.getClient().executeBoundStatement(
					CassandraRuntimeRepository.RUNNING_FLOWS_INSERT_STMT, UUID.randomUUID(), USER_NAME,
					"Test Script " + (cnt + 1), parameters, startDate, startTime, callStack);

		}
	}

	public static void insertCompletedFlows(int recordsCount, final Date initTime) {
		insertCompletedFlows(recordsCount, initTime, EMPTY_LIST, EMPTY_LIST);
	}

	public static void insertCompletedFlows(int recordsCount, final Date initTime, List<String> parameters,
			List<String> callStack) {

		final String startDate = TOKEN_DATE_FORMAT.format(initTime);

		for (int cnt = 0; cnt < recordsCount; cnt++) {
			Date startTime = DateHelper.addMinutes(initTime, cnt);
			Date endTime = startTime;

			CassandraEmbeddedServer.getClient().executeBoundStatement(
					CassandraRuntimeRepository.COMPLETED_FLOWS_INSERT_STMT, UUID.randomUUID(), USER_NAME,
					"Test Script " + (cnt + 1), parameters, startDate, startTime, callStack, endTime);

		}
	}

	public static void clearData() {
		CassandraEmbeddedServer.getClient().query("TRUNCATE failed_flows");
		CassandraEmbeddedServer.getClient().query("TRUNCATE running_flows");
		CassandraEmbeddedServer.getClient().query("TRUNCATE completed_flows");
	}

}
