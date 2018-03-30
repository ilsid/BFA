package com.ilsid.bfa.main;

import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.ResourceCollection;

import com.google.inject.servlet.GuiceFilter;
import com.ilsid.bfa.service.server.ApplicationConfig;
import com.ilsid.bfa.service.server.ConfigUtil;


public class HttpServer {

	private static final String PORT_PROPERTY = "bfa.http.server.port";

	private static final String DEFAULT_PORT_VALUE = "8092";

	private static final String CONTEXT_ROOT = "";

	private static final String WEBAPP_ROOT = "web/";

	private static Map<String, String> configuration;

	private static org.eclipse.jetty.server.Server server;

	
	public static void start() throws Exception {
		configuration = ConfigUtil.getApplicationSettings();
		server = new org.eclipse.jetty.server.Server(getServerPort());

		ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);

		contextHandler.addEventListener(new ApplicationConfig());
		contextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		contextHandler.setContextPath(CONTEXT_ROOT);
		contextHandler.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		// DefaultServlet is required to avoid 404 errors
		contextHandler.addServlet(DefaultServlet.class, "/");
		
		registerWebResources(contextHandler);

		server.setHandler(contextHandler);
		server.start();
	}

	public static void join() throws Exception {
		if (server != null) {
			server.join();
		}
	}

	public static void stop() throws Exception {
		if (server != null && server.isStarted()) {
			server.stop();
			configuration = null;
		}
	}

	private static void registerWebResources(ServletContextHandler contextHandler) {
		ResourceCollection resources = new ResourceCollection(new String[] { WEBAPP_ROOT });
		contextHandler.setBaseResource(resources);
	}

	private static int getServerPort() {
		String serverPortValue = configuration.get(PORT_PROPERTY);
		if (serverPortValue == null) {
			serverPortValue = DEFAULT_PORT_VALUE;
		}

		int serverPort;
		try {
			serverPort = Integer.parseInt(serverPortValue);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Incorrect server port value: " + serverPortValue);
		}

		return serverPort;
	}

}
