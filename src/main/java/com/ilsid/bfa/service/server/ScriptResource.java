package com.ilsid.bfa.service.server;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ilsid.bfa.common.ExceptionUtil;
import com.ilsid.bfa.manager.ManagementException;
import com.ilsid.bfa.manager.ScriptManager;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.ScriptDTO;

/**
 * Provides the script related services.
 * 
 * @author illia.sydorovych
 *
 */
@Path(Paths.SCRIPT_SERVICE_ROOT)
public class ScriptResource {

	private ScriptManager scriptManager;

	/**
	 * Creates the script and saves it in the code repository.
	 * 
	 * @param script
	 *            the script data
	 * @return response to the client. The response can be {@link Status#OK} in case of successful script creation or
	 *         {@link Status#INTERNAL_SERVER_ERROR} in case of any operation failure.
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_CREATE_OPERATION)
	//FIXME: add error logging
	public Response create(ScriptDTO script) {
		try {
			scriptManager.createScript(script.getName(), script.getBody());
		} catch (ManagementException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtil.getExceptionMessageChain(e))
					.build();
		}

		return Response.status(Status.OK).build();
	}

	@Inject
	public void setScriptManager(ScriptManager scriptManager) {
		this.scriptManager = scriptManager;
	}

}
