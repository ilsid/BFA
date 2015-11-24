package com.ilsid.bfa.service.server;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

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
	
	protected static class TestApplicationConfig extends GuiceServletContextListener {

		private static final String LOGGER_NAME = "test_logger";

		private final Class<? extends CodeRepository> repositoryClass;
		
		private final Map<String, String> repositoryConfig;
		
		public TestApplicationConfig(Class<? extends CodeRepository> repositoryClass,
				Map<String, String> repositoryConfig) {
			this.repositoryClass = repositoryClass;
			this.repositoryConfig = repositoryConfig;
		}
		
		@Override
		protected Injector getInjector() {
			return Guice.createInjector(new JerseyServletModule() {

				@Override
				protected void configureServlets() {
					bind(CodeRepository.class).to(repositoryClass);

					requestStaticInjection(DynamicClassLoader.class);

					Map<String, String> webConfig = new HashMap<>();
					// org.codehaus.jackson.jaxrs package contains the provider for POJO JSON mapping
					webConfig.put(PackagesResourceConfig.PROPERTY_PACKAGES,
							"com.ilsid.bfa.service.server; org.codehaus.jackson.jaxrs");
					webConfig.put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE.toString());

					serve("/*").with(GuiceContainer.class, webConfig);
				}
				
				@Provides
				@RepositoryConfig
				protected Map<String, String> provideRepositoryConfiguration() {
					return repositoryConfig;
				}

				@Provides
				@WebAppLogger
				protected Logger provideLogger() {
					return LoggerFactory.getLogger(LOGGER_NAME);
				}
			});
		}

	}

}
