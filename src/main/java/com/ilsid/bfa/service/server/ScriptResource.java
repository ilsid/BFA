package com.ilsid.bfa.service.server;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
	 * @return the {@link Status#OK} response.
	 * @throws WebApplicationException
	 *             <ul>
	 *             <li>if the script itself or any of its expressions can't be compiled or persisted</li>
	 *             <li>if the script with such name already exist in the repository</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_CREATE_OPERATION)
	public Response create(ScriptDTO script) {
		try {
			scriptManager.createScript(script.getName(), script.getBody());
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_CREATE_SERVICE, e);
		}

		return Response.status(Status.OK).build();
	}

	/**
	 * Updates the existing script in the code repository.
	 * 
	 * @param script
	 *            the script data
	 * @return the {@link Status#OK} response.
	 * @throws WebApplicationException
	 *             <ul>
	 *             <li>if the script itself or any of its expressions can't be compiled or persisted</li>
	 *             <li>if the script with such name does not exist in the repository</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_UPDATE_OPERATION)
	public Response update(ScriptDTO script) {
		try {
			scriptManager.updateScript(script.getName(), script.getBody());
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_UPDATE_SERVICE, e);
		}

		return Response.status(Status.OK).build();
	}

	@Inject
	public void setScriptManager(ScriptManager scriptManager) {
		this.scriptManager = scriptManager;
	}

}
