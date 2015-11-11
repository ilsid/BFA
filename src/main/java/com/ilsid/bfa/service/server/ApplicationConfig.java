package com.ilsid.bfa.service.server;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.GuiceServletContextListener;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.ilsid.bfa.persistence.filesystem.FSCodeRepository;
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
// FIXME: remove hardcoded config and CodeRepository implementation
public class ApplicationConfig extends GuiceServletContextListener {

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new JerseyServletModule() {

			@Override
			protected void configureServlets() {
				bind(CodeRepository.class).to(FSCodeRepository.class);

				Map<String, String> webConfig = new HashMap<>();
				// org.codehaus.jackson.jaxrs package contains the provider for POJO JSON mapping
				webConfig.put(PackagesResourceConfig.PROPERTY_PACKAGES,
						"com.ilsid.bfa.service.server;org.codehaus.jackson.jaxrs");
				webConfig.put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE.toString());

				serve("/*").with(GuiceContainer.class, webConfig);
			}

			@Provides
			@RepositoryConfig
			protected Map<String, String> provideRepositoryConfiguration() {
				Map<String, String> result = new HashMap<>();
				result.put("bfa.persistence.fs.root_dir", "d:\\temp\\glassfish\\code_repository");

				return result;
			}
			
			@Provides
			@WebAppLogger
			protected Logger provideLogger() {
				return LoggerFactory.getLogger("error_logger");
			}
		});
	}

}
