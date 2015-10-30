package com.ilsid.bfa.service.server;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

/**
 * REST Application configuration.
 * @see <i>web.xml</i>
 * 
 * @author illia.sydorovych
 *
 */
public class ApplicationConfig extends GuiceServletContextListener {

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new JerseyServletModule() {

			@Override
			protected void configureServlets() {
				Map<String, String> config = new HashMap<>();
				// org.codehaus.jackson.jaxrs package contains the provider for POJO JSON mapping
				config.put(PackagesResourceConfig.PROPERTY_PACKAGES,
						"com.ilsid.bfa.service.server;org.codehaus.jackson.jaxrs");
				config.put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE.toString());

				serve("/*").with(GuiceContainer.class, config);
			}
		});
	}

}
