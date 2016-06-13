package com.ilsid.bfa.service.server;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.FileUtils;
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
import com.ilsid.bfa.IntegrationTestConstants;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.persistence.ActionClassLoader;
import com.ilsid.bfa.action.persistence.ActionRepository;
import com.ilsid.bfa.common.LoggingConfig;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.PersistenceLogger;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.ilsid.bfa.persistence.ScriptingRepository;
import com.ilsid.bfa.persistence.orientdb.OrientdbEmbeddedServer;
import com.ilsid.bfa.persistence.orientdb.OrientdbResourceManager;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;
import com.ilsid.bfa.runtime.persistence.orientdb.OrientdbRuntimeRepository;
import com.ilsid.bfa.script.ClassCompiler;
import com.ilsid.bfa.script.ScriptLogger;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.multipart.impl.MultiPartWriter;

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
		config.getClasses().add(MultiPartWriter.class);
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

	public static void startDatabaseServer() throws Exception {
		// Copy initial clean database to the testing directory
		FileUtils.copyDirectory(TestConstants.INIT_DATABASE_DIR, IntegrationTestConstants.DATABASE_DIR);
		System.setProperty(TestConstants.ORIENTDB_HOME_PROPERTY, IntegrationTestConstants.ORIENTDB_HOME_DIR.getPath());
		OrientdbEmbeddedServer.startup();
	}

	public static void stopDatabaseServer() throws Exception {
		System.getProperties().remove(TestConstants.ORIENTDB_HOME_PROPERTY);
		OrientdbEmbeddedServer.shutdown();
	}

	private static String getRootURL() {
		return LOCALHOST_URL + ":" + serverPort + CONTEXT_ROOT + "/";
	}

	protected static class TestApplicationConfig extends GuiceServletContextListener {

		private static final String LOGGER_NAME = "test_logger";

		private final Class<? extends ScriptingRepository> scriptingRepositoryClass;

		private final Class<? extends ActionRepository> actionRepositoryClass;

		private final Map<String, String> repositoryConfig;

		public TestApplicationConfig(Class<? extends ScriptingRepository> scriptingRepositoryClass,
				Class<? extends ActionRepository> actionRepositoryClass, Map<String, String> repositoryConfig) {

			this.scriptingRepositoryClass = scriptingRepositoryClass;
			this.actionRepositoryClass = actionRepositoryClass;
			this.repositoryConfig = repositoryConfig;
		}

		@Override
		public void contextDestroyed(javax.servlet.ServletContextEvent servletContextEvent) {
			super.contextDestroyed(servletContextEvent);
			OrientdbResourceManager.releaseResources();
		}

		@Override
		protected Injector getInjector() {
			return Guice.createInjector(new JerseyServletModule() {

				@Override
				protected void configureServlets() {
					bind(ScriptingRepository.class).to(scriptingRepositoryClass).in(Singleton.class);
					bind(ActionRepository.class).to(actionRepositoryClass).in(Singleton.class);
					bind(RuntimeRepository.class).to(OrientdbRuntimeRepository.class).in(Singleton.class);

					requestStaticInjection(DynamicClassLoader.class);
					requestStaticInjection(ActionClassLoader.class);
					requestStaticInjection(ClassCompiler.class);

					Map<String, String> webConfig = new HashMap<>();
					// org.codehaus.jackson.jaxrs package contains the provider for POJO JSON mapping
					webConfig.put(PackagesResourceConfig.PROPERTY_PACKAGES,
							"com.ilsid.bfa.service.server; org.codehaus.jackson.jaxrs");
					webConfig.put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE.toString());

					serve("/*").with(GuiceContainer.class, webConfig);
				}

				@Provides
				@Singleton
				@RepositoryConfig
				protected Map<String, String> provideRepositoryConfiguration() {
					return repositoryConfig;
				}

				@Provides
				@Singleton
				@LoggingConfig
				protected Map<String, String> provideLoggingConfiguration() {
					return repositoryConfig;
				}

				@Provides
				@Singleton
				@WebAppLogger
				protected Logger provideWebAppLogger() {
					return LoggerFactory.getLogger(LOGGER_NAME);
				}

				@Provides
				@Singleton
				@ScriptLogger
				protected Logger provideScriptLogger() {
					return LoggerFactory.getLogger(LOGGER_NAME);
				}

				@Provides
				@Singleton
				@PersistenceLogger
				protected Logger providePersistenceLogger() {
					return LoggerFactory.getLogger(LOGGER_NAME);
				}

			});
		}

	}

}
