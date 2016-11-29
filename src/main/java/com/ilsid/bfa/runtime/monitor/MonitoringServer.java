package com.ilsid.bfa.runtime.monitor;

import java.io.IOException;
import java.net.Inet4Address;

import javax.websocket.DeploymentException;

import org.glassfish.tyrus.server.Server;

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

	public static void main(String[] args) throws MonitoringException, IOException {
		// System.out.println(Inet4Address.getLocalHost().getHostAddress());
		System.out.println(Inet4Address.getLocalHost().getHostAddress());
		MonitoringServer.start(Inet4Address.getLocalHost().getHostAddress(), 8025);
		System.in.read();
		// MonitorServer.stop();
	}

}
