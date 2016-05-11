package com.ilsid.bfa.service.server;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

/**
 * Intercepts {@link ResourceException} exceptions raised by resource methods.
 * 
 * @author illia.sydorovych
 *
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<ResourceException> {

	private static final String ERROR_MESSAGE_TEMPLATE = "Service [{}] failed";

	private Logger logger;

	/**
	 * Logs the exception if the logger is defined and returns response with the exception's status and entity.
	 * 
	 * @return response for the given exception
	 */
	public Response toResponse(ResourceException exception) {
		if (logger != null) {
			logger.error(ERROR_MESSAGE_TEMPLATE, exception.getPath(), exception.getActualCause());
		}

		return Response.status(exception.getStatus()).entity(exception.getEntity()).build();
	}

	/**
	 * Defines the logger implementation.
	 * 
	 * @param logger
	 *            the logger instance
	 */
	@Inject
	public void setLogger(@WebAppLogger Logger logger) {
		this.logger = logger;
	}

}
