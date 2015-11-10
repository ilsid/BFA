package com.ilsid.bfa.service.server;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ilsid.bfa.manager.ManagementException;
import com.ilsid.bfa.manager.ScriptManager;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.ScriptDTO;

/**
 * Provides the script administration related services.
 * 
 * @author illia.sydorovych
 *
 */
@Path(Paths.SCRIPT_SERVICE_ADMIN_ROOT)
// FIXME: Handle non-default script groups
public class ScriptAdminResource {

	private ScriptManager scriptManager;

	/**
	 * Creates the script and saves it in the code repository. If the script's group is not specified, it is created
	 * within the Default Group.
	 * 
	 * @param script
	 *            the script data. The name and body must be specified
	 * @return the {@link Status#OK} response.
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the script itself or any of its expressions can't be compiled or persisted</li>
	 *             <li>if the script with the specified name already exist in the specified group</li>
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
	 * Updates the existing script in the code repository. If the script's group is not specified, it is searched within
	 * the Default Group.
	 * 
	 * @param script
	 *            the script data. The name and body must be specified
	 * @return the {@link Status#OK} response.
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the script itself or any of its expressions can't be compiled or persisted</li>
	 *             <li>if the script with the specified name does not exist in the the specified group</li>
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

	/**
	 * Loads the source code for the given script. If the script's group is not specified, it is searched within the
	 * Default Group.
	 * 
	 * @param script
	 *            the script data. The name must be specified
	 * @return the script's source code
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the script with the specified name does not exist in the specified group</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Path(Paths.SCRIPT_GET_SOURCE_OPERATION)
	public Response getSource(ScriptDTO script) {
		String scriptSource;
		try {
			scriptSource = scriptManager.getScriptSourceCode(script.getName());
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_GET_SOURCE_SERVICE, e);
		}

		return Response.status(Status.OK).entity(scriptSource).build();
	}

	@Inject
	public void setScriptManager(ScriptManager scriptManager) {
		this.scriptManager = scriptManager;
	}

}
