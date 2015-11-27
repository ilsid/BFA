package com.ilsid.bfa.service.server;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ilsid.bfa.manager.ManagementException;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.ScriptAdminParams;

/**
 * Provides the script administration services.
 * 
 * @author illia.sydorovych
 *
 */
@Path(Paths.SCRIPT_SERVICE_ADMIN_ROOT)
// FIXME: Handle non-default script groups
public class ScriptAdminResource extends AbstractAdminResource {

	/**
	 * Creates the script and saves it in the code repository. If the script's group is not specified, it is created
	 * within the Default Group.
	 * 
	 * @param script
	 *            the script data. The name and body must be specified
	 * @return the {@link Status#OK} response.
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the script's name or body are not specified</li>
	 *             <li>if the script itself or any of its expressions can't be compiled or persisted</li>
	 *             <li>if the script with the specified name already exist in the specified group</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_CREATE_OPERATION)
	public Response create(ScriptAdminParams script) {
		try {
			validateNonNullNameAndBody(script);
			scriptManager.createScript(script.getName(), script.getBody());
		} catch (IllegalArgumentException | ManagementException e) {
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
	 *             <li>if the script's name or body are not specified</li>
	 *             <li>if the script itself or any of its expressions can't be compiled or persisted</li>
	 *             <li>if the script with the specified name does not exist in the the specified group</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_UPDATE_OPERATION)
	public Response update(ScriptAdminParams script) {
		try {
			validateNonNullNameAndBody(script);
			scriptManager.updateScript(script.getName(), script.getBody());
		} catch (IllegalArgumentException | ManagementException e) {
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
	 * @return the response containing the script's source code (as plain text)
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the script's name is not specified</li>
	 *             <li>if the script with the specified name does not exist in the specified group</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Path(Paths.SCRIPT_GET_SOURCE_OPERATION)
	public Response getSource(ScriptAdminParams script) {
		String scriptSource;
		try {
			validateNonNullName(script);
			scriptSource = scriptManager.getScriptSourceCode(script.getName());
		} catch (IllegalArgumentException | ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_GET_SOURCE_SERVICE, e);
		}

		return Response.status(Status.OK).entity(scriptSource).build();
	}

}
