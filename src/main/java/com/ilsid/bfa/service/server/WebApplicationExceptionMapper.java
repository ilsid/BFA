package com.ilsid.bfa.service.server;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.ilsid.bfa.common.ExceptionUtil;

/**
 * Intercepts {@link WebApplicationException} exceptions raised by resource methods.
 * 
 * @author illia.sydorovych
 *
 */
@Provider
// FIXME: add error logging
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

	/**
	 * Logs the exception and returns {@link Status#INTERNAL_SERVER_ERROR} response containing the list of exception
	 * messages.
	 * 
	 * @return {@link Status#INTERNAL_SERVER_ERROR} response
	 */
	public Response toResponse(WebApplicationException exception) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(ExceptionUtil.getExceptionMessageChain(exception.getCause())).build();

	}

}
