package com.ilsid.bfa.runtime.monitor;

import javax.inject.Inject;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

/**
 * Provides scripting runtime information in real-time via WebSockets interface.
 * 
 * @author illia.sydorovych
 *
 */
@ServerEndpoint(value = MonitoringServer.MONITOR_END_POINT)
public class MonitorEndpoint {

	private RuntimeRepository repository;

	@OnMessage
	public String getResponse(String message, Session session) {
		// TODO: put actual implementation
		return session.getId() + ": " + message;
	}

	/**
	 * Defines a code repository implementation.
	 * 
	 * @param repository
	 *            a code repository
	 */
	@Inject
	public void setRepository(RuntimeRepository repository) {
		this.repository = repository;
	}

}
