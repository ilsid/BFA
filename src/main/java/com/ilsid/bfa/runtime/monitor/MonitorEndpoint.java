package com.ilsid.bfa.runtime.monitor;

import java.io.IOException;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.ilsid.bfa.common.JsonUtil;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.QueryPage;
import com.ilsid.bfa.persistence.QueryPagingOptions;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.service.dto.ScriptRuntimeQuery;

/**
 * Provides scripting runtime information in real-time via WebSockets interface.
 * 
 * @author illia.sydorovych
 *
 */
@ServerEndpoint(value = MonitoringServer.MONITOR_END_POINT)
public class MonitorEndpoint {

	@OnMessage
	public String getResponse(String message, Session session) {
		ScriptRuntimeQuery query;
		try {
			query = JsonUtil.toObject(message, ScriptRuntimeQuery.class);
		} catch (IOException e) {
			e.printStackTrace();
			return "Query paring error: " + e.getMessage();
		}

		final ScriptRuntimeCriteria criteria = query.getCriteria();

		QueryPagingOptions pagingOptions = new QueryPagingOptions();
		pagingOptions.setResultsPerPage(query.getResultsPerPage());

		QueryPage<ScriptRuntimeDTO> result;
		String json;
		try {
			result = MonitoringServer.getRepository().fetch(criteria, pagingOptions);
			json = JsonUtil.toJsonString(result);
		} catch (PersistenceException | IOException e) {
			e.printStackTrace();
			return "Query execution error: " + e.getMessage();
		}

		// return session.getId() + ": " + response;
		return json;
	}
}
