package com.ilsid.bfa.service.server;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

	private final static String GROUP_PARAM_NAME = "group";

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
		validateNonNullNameAndBody(Paths.SCRIPT_CREATE_SERVICE, script);
		try {
			scriptManager.createScript(script.getName(), script.getBody(), script.getTitle());
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
		validateNonNullNameAndBody(Paths.SCRIPT_UPDATE_SERVICE, script);
		try {
			scriptManager.updateScript(script.getName(), script.getBody(), script.getTitle());
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
		validateNonNullName(Paths.SCRIPT_GET_SOURCE_SERVICE, script);
		String scriptSource;
		try {
			scriptSource = scriptManager.getScriptSourceCode(script.getName());
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_GET_SOURCE_SERVICE, e);
		}

		return Response.status(Status.OK).entity(scriptSource).build();
	}

	/**
	 * Loads meta-data objects for the scripting groups under the given parent group. If the parent group name is
	 * <code>null</code> then meta-data objects for top-level groups are returned.
	 * 
	 * @param groupName
	 *            name of the parent group or <code>null</code>
	 * @return a list of meta-data objects. Each object is represented by <{@link Map} instance.
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if no parent group with the given name exists</li>
	 *             <li>if parent group name is <code>null</code> and no top-level groups exist</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_GET_GROUPS_OPERATION)
	public Response getGroups(@QueryParam("parent") String groupName) {
		List<Map<String, String>> metas;
		try {
			metas = scriptManager.getTopLevelGroupMetadatas();
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_GET_GROUPS_SERVICE, e);
		}

		return Response.status(Status.OK).entity(metas).build();
	}

	/**
	 * Loads meta-data items for the scripts in the given group.
	 * 
	 * @param groupName
	 *            name of the group
	 * @return a list of meta-data items or an empty list, if no scripts found or such group does not exist. Each item
	 *         is represented by <{@link Map} instance.
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the passed group name is <code>null</code></li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_GET_ITEMS_OPERATION)
	public Response getItems(@QueryParam(GROUP_PARAM_NAME) String groupName) {
		validateNonNullParameter(Paths.SCRIPT_GET_ITEMS_SERVICE, GROUP_PARAM_NAME, groupName);
		List<Map<String, String>> metas;
		try {
			metas = scriptManager.getScriptMetadatas(groupName);
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_GET_ITEMS_SERVICE, e);
		}

		return Response.status(Status.OK).entity(metas).build();
	}

}
