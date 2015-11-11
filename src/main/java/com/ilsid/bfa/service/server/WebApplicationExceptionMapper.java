package com.ilsid.bfa.service.server;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import com.ilsid.bfa.common.ExceptionUtil;

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
	 * Logs the exception if the logger is defined and returns {@link Status#INTERNAL_SERVER_ERROR} response containing
	 * the list of exception messages.
	 * 
	 * @return {@link Status#INTERNAL_SERVER_ERROR} response
	 */
	public Response toResponse(ResourceException exception) {
		Throwable actualException = exception.getCause();
		if (logger != null) {
			logger.error(ERROR_MESSAGE_TEMPLATE, exception.getMessage(), actualException);
		}

		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(ExceptionUtil.getExceptionMessageChain(actualException)).build();

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
