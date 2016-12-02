package com.ilsid.bfa.runtime.monitor;

import java.io.IOException;
import java.net.Inet4Address;

import javax.inject.Inject;
import javax.websocket.DeploymentException;

import org.glassfish.tyrus.server.Server;

import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

/**
 * WebSockets server for the runtime monitoring.
 * 
 * @author illia.sydorovych
 *
 */
public class MonitoringServer {

	/**
	 * Server's context path.
	 */
	public static final String CONTEXT_PATH = "/bfa";

	public static final String MONITOR_END_POINT = "/monitor";

	private static Server server;

	private static RuntimeRepository repository;

	/**
	 * Starts the server.
	 * 
	 * @throws MonitoringException
	 *             if the server can't be started
	 */
	public static synchronized void start(String host, int port) throws MonitoringException {
		if (server == null) {
			server = new Server(host, port, CONTEXT_PATH, null, MonitorEndpoint.class);
			try {
				server.start();
			} catch (DeploymentException e) {
				throw new MonitoringException("Failed to start the monitoring server", e);
			}
		}
	}

	/**
	 * Stops the server.
	 */
	public static synchronized void stop() {
		if (server != null) {
			server.stop();
		}
	}

	/**
	 * Defined a runtime repository implementation.
	 * 
	 * @param repository
	 *            repository implementation
	 */
	@Inject
	public static void setRepository(RuntimeRepository repository) {
		MonitoringServer.repository = repository;
	}

	static RuntimeRepository getRepository() {
		return repository;
	}

	public static void main(String[] args) throws MonitoringException, IOException {
		System.out.println(Inet4Address.getLocalHost().getCanonicalHostName());
		MonitoringServer.start(Inet4Address.getLocalHost().getCanonicalHostName(), 8027);
		System.in.read();
		MonitoringServer.stop();
	}

}
