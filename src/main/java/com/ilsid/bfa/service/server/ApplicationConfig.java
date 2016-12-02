package com.ilsid.bfa.service.server;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.GuiceServletContextListener;
import com.ilsid.bfa.action.persistence.ActionClassLoader;
import com.ilsid.bfa.action.persistence.ActionRepository;
import com.ilsid.bfa.action.persistence.filesystem.FilesystemActionRepository;
import com.ilsid.bfa.common.LoggingConfig;
import com.ilsid.bfa.manager.ManagerConfig;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.PersistenceLogger;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.ilsid.bfa.persistence.ScriptingRepository;
import com.ilsid.bfa.persistence.cassandra.CassandraResourceManager;
import com.ilsid.bfa.persistence.filesystem.FilesystemScriptingRepository;
import com.ilsid.bfa.runtime.monitor.MonitoringServer;
import com.ilsid.bfa.runtime.monitor.MonitoringServerConfig;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;
import com.ilsid.bfa.runtime.persistence.cassandra.CassandraRuntimeRepository;
import com.ilsid.bfa.script.ClassCompiler;
import com.ilsid.bfa.script.ScriptLogger;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

/**
 * REST Application configuration.
 * 
 * @see <i>web.xml</i>
 * 
 * @author illia.sydorovych
 *
 */
// FIXME: remove hardcoded Repository implementations
public class ApplicationConfig extends GuiceServletContextListener {

	private static final String PERSISTENCE_LOGGER_NAME = "persistence_logger";

	private static final String WEBAPP_LOGGER_NAME = "webapp_logger";

	private static final String SCRIPT_LOGGER_NAME = "script_logger";

	@Override
	public void contextDestroyed(javax.servlet.ServletContextEvent servletContextEvent) {
		super.contextDestroyed(servletContextEvent);
		CassandraResourceManager.releaseResources();
		MonitoringServer.stop();
	}

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new JerseyServletModule() {

			@Override
			protected void configureServlets() {
				bind(ScriptingRepository.class).to(FilesystemScriptingRepository.class).asEagerSingleton();
				bind(ActionRepository.class).to(FilesystemActionRepository.class).asEagerSingleton();
				bind(RuntimeRepository.class).to(CassandraRuntimeRepository.class).asEagerSingleton();

				requestStaticInjection(DynamicClassLoader.class);
				requestStaticInjection(ActionClassLoader.class);
				requestStaticInjection(ClassCompiler.class);
				requestStaticInjection(MonitoringServer.class);

				Map<String, String> webConfig = new HashMap<>();
				// org.codehaus.jackson.jaxrs package contains the provider for POJO JSON mapping
				webConfig.put(PackagesResourceConfig.PROPERTY_PACKAGES,
						"com.ilsid.bfa.service.server;org.codehaus.jackson.jaxrs");
				webConfig.put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE.toString());

				serve("/*").with(GuiceContainer.class, webConfig);
			}

			@Provides
			@Singleton
			@RepositoryConfig
			protected Map<String, String> provideRepositoryConfiguration() {
				return getApplicationConfig();
			}

			@Provides
			@Singleton
			@LoggingConfig
			protected Map<String, String> provideLoggingConfiguration() {
				return getApplicationConfig();
			}

			@Provides
			@Singleton
			@ManagerConfig
			protected Map<String, String> provideManagerConfiguration() {
				return getApplicationConfig();
			}

			@Provides
			@Singleton
			@MonitoringServerConfig
			protected Map<String, String> provideMonitoringServerConfiguration() {
				return getApplicationConfig();
			}

			@Provides
			@Singleton
			@WebAppLogger
			protected Logger provideWebAppLogger() {
				return LoggerFactory.getLogger(WEBAPP_LOGGER_NAME);
			}

			@Provides
			@Singleton
			@ScriptLogger
			protected Logger provideScriptLogger() {
				return LoggerFactory.getLogger(SCRIPT_LOGGER_NAME);
			}

			@Provides
			@Singleton
			@PersistenceLogger
			protected Logger providePersistenceLogger() {
				return LoggerFactory.getLogger(PERSISTENCE_LOGGER_NAME);
			}

			private Map<String, String> getApplicationConfig() {
				return ConfigUtil.getApplicationSettings();
			}
		});
	}

}
