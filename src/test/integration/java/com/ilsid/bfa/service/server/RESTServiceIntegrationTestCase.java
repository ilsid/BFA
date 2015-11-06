package com.ilsid.bfa.service.server;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.google.inject.servlet.GuiceFilter;
import com.ilsid.bfa.BaseUnitTestCase;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public abstract class RESTServiceIntegrationTestCase extends BaseUnitTestCase {

	private static final String PORT_PROPERTY = "test.http.server.port";

	private static final String LOCALHOST_URL = "http://localhost";

	private static final String CONTEXT_ROOT = "/bfa";

	private static int serverPort = 8082;

	private static String rootURL = getRootURL();

	private static Server server;

	private static Client client;

	protected WebResource getWebResource(String path) {
		return client.resource(rootURL + path);
	}

	public static void startWebServer(ServletContextListener applicationConfig) throws Exception {
		ClientConfig config = new DefaultClientConfig();
		config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		client = Client.create(config);

		String serverPortConfigValue = System.getProperty(PORT_PROPERTY);
		if (serverPortConfigValue != null) {
			serverPort = Integer.parseInt(serverPortConfigValue);
			rootURL = getRootURL();
		}

		server = new Server(serverPort);

		ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		contextHandler.addEventListener(applicationConfig);
		contextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
		contextHandler.setContextPath(CONTEXT_ROOT);
		// DefaultServlet is required to avoid 404 errors
		contextHandler.addServlet(DefaultServlet.class, "/");

		server.setHandler(contextHandler);
		server.start();
	}

	public static void stopWebServer() throws Exception {
		if (server != null && server.isStarted()) {
			server.stop();
		}
	}

	private static String getRootURL() {
		return LOCALHOST_URL + ":" + serverPort + CONTEXT_ROOT + "/";
	}

}
