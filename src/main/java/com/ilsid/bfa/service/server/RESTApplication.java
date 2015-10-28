package com.ilsid.bfa.service.server;

import java.util.HashMap;
import java.util.Map;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;

/**
 * Jersey REST application entry point.
 * 
 * @author illia.sydorovych
 *
 */
public class RESTApplication extends PackagesResourceConfig {

	private static final Map<String, Object> config = new HashMap<>();

	static {
		// org.codehaus.jackson.jaxrs package contains the provider for POJO JSON mapping  
		config.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.ilsid.bfa.service.server;org.codehaus.jackson.jaxrs");
		config.put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

	}

	public RESTApplication() {
		super(config);
	}

}
