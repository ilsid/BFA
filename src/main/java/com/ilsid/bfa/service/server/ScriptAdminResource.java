package com.ilsid.bfa.service.server;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.graph.MermaidConverter;
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
public class ScriptAdminResource extends AbstractAdminResource {

	private static final String FLOW_DIAGRAM_PARAM_NAME = "flow diagram";

	/**
	 * Creates the script and saves it in the code repository. If the script's group is not specified, it is created
	 * within the Default Group.
	 * 
	 * @param script
	 *            the script data. The name must be specified. If script body is not defined then flow diagram
	 *            representation is used for the script generation.
	 * @return the {@link Status#OK} response
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
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.CREATE_OPERATION)
	public Response create(ScriptAdminParams script) {
		validateNonEmptyName(Paths.SCRIPT_CREATE_SERVICE, script);
		try {
			final String body = script.getBody();
			final String flowDiagram = script.getFlowDiagram();
			if (!isEmpty(body)) {
				scriptManager.createScript(script.getName(), body, flowDiagram);
			} else {
				validateNonEmptyParameter(Paths.SCRIPT_CREATE_SERVICE, FLOW_DIAGRAM_PARAM_NAME, flowDiagram);
				scriptManager.createScript(script.getName(), flowDiagram);
			}
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
	 *            the script data. The name must be specified. If script body is not defined then flow diagram
	 *            representation is used for the script generation.
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
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.UPDATE_OPERATION)
	public Response update(ScriptAdminParams script) {
		validateNonEmptyName(Paths.SCRIPT_UPDATE_SERVICE, script);
		try {
			final String body = script.getBody();
			if (!isEmpty(body)) {
				scriptManager.updateScript(script.getName(), body, script.getFlowDiagram());
			} else {
				scriptManager.updateScript(script.getName(), script.getFlowDiagram());
			}
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_UPDATE_SERVICE, e);
		}

		return Response.status(Status.OK).build();
	}

	/**
	 * Deletes script from the code repository. If the script's group is not specified, it is searched within the
	 * Default Group.
	 * 
	 * @param scriptName
	 *            the script name
	 * @return the {@link Status#OK} response.
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the script's group does not exists in the repository</li>
	 *             <li>if the script with such name does not exist in the repository within the given group</li>
	 *             <li>if the script itself can't be deleted for any reason</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.DELETE_OPERATION)
	public Response delete(String scriptName) {
		validateNonEmptyParameter(Paths.DELETE_OPERATION, NAME_PARAM, scriptName);

		try {
			scriptManager.deleteScript(scriptName);
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_DELETE_SERVICE, e);
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
	@Path(Paths.GET_SOURCE_OPERATION)
	public Response getSource(ScriptAdminParams script) {
		validateNonEmptyName(Paths.SCRIPT_GET_SOURCE_SERVICE, script);
		String scriptSource;
		try {
			scriptSource = scriptManager.getScriptSourceCode(script.getName());
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_GET_SOURCE_SERVICE, e);
		}

		return Response.status(Status.OK).entity(scriptSource).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Path(Paths.GET_DIAGRAM_OPERATION)
	public Response getDiagram(ScriptAdminParams script) {
		validateNonEmptyName(Paths.SCRIPT_GET_DIAGRAM_SERVICE, script);
		String diagram;
		try {
			diagram = scriptManager.getScriptDiagram(script.getName());
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_GET_DIAGRAM_SERVICE, e);
		}

		if (diagram == null) {
			diagram = StringUtils.EMPTY;
		}

		return Response.status(Status.OK).entity(diagram).build();
	}

	/**
	 * Loads meta-data items for the members of the given group. The members can be scripts or/and script groups. If
	 * <code>groupName</code> parameter equals {@link Metadata#ROOT_PARENT_NAME} then top-level group items are loaded.
	 * 
	 * @param groupName
	 *            name of the group
	 * @return a list of meta-data items or an empty list, if no members found or such group does not exist. Each item
	 *         is represented by <{@link Map} instance.
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the passed group name is <code>null</code></li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.GET_ITEMS_OPERATION)
	public Response getItems(String groupName) {
		validateNonEmptyParameter(Paths.SCRIPT_GET_ITEMS_SERVICE, GROUP_PARAM, groupName);
		List<Map<String, String>> metas;
		try {
			if (groupName.equals(Metadata.ROOT_PARENT_NAME)) {
				metas = scriptManager.getTopLevelScriptGroupMetadatas();
			} else {
				metas = scriptManager.getChildrenScriptGroupMetadatas(groupName);
			}
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_GET_ITEMS_SERVICE, e);
		}

		return Response.status(Status.OK).entity(metas).build();
	}

	/**
	 * Creates new script group in the code repository. The group name can be simple or complex. The simple group name
	 * is like <i>My Group</i>. It is treated as a top-level group. The complex group name can be like <i>Grand Parent
	 * Group::Parent Group::My Group</i>.
	 * 
	 * @param groupName
	 *            the name of group to create
	 * @return the {@link Status#OK} response
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the passed group name is empty</li>
	 *             <li>if such group already exists in the repository</li>
	 *             <li>if parent group does not exists in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.CREATE_GROUP_OPERATION)
	public Response createGroup(String groupName) {
		validateNonEmptyParameter(Paths.CREATE_GROUP_OPERATION, GROUP_PARAM, groupName);

		try {
			scriptManager.createScriptGroup(groupName);
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_CREATE_GROUP_SERVICE, e);
		}

		return Response.status(Status.OK).build();
	}

	/**
	 * Loads meta-data about script input parameters (Name/Type entries).
	 * 
	 * @param scriptName
	 *            name of the script
	 * @return the input parameters meta-data represented as {@link Map} with Name/Type entries or an empty {@link Map}
	 *         if the script has no input parameters
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the script with such name does not exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.GET_INPUT_PARAMS_OPERATION)
	public Response getInputParameters(String scriptName) {
		validateNonEmptyParameter(Paths.SCRIPT_GET_INPUT_PARAMS_SERVICE, NAME_PARAM, scriptName);
		Map<String, String> params;
		try {
			params = scriptManager.getScriptParametersMetadata(scriptName);
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_GET_INPUT_PARAMS_SERVICE, e);
		}

		return Response.status(Status.OK).entity(params).build();
	}

	/**
	 * Loads a flow chart representation for the given script. The representation has
	 * <a href="https://github.com/knsv/mermaid">mermaid</a> format. If the script's group is not specified, it is
	 * searched within the Default Group.
	 * 
	 * @param script
	 *            the script data. The name must be specified
	 * @return the response containing the script's flow chart representation (as plain text)
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
	@Path(Paths.GET_FLOW_CHART_OPERATION)
	public Response getFlowChart(ScriptAdminParams script) {
		validateNonEmptyName(Paths.SCRIPT_GET_FLOW_CHART_SERVICE, script);
		String scriptSource;
		try {
			scriptSource = scriptManager.getScriptSourceCode(script.getName());
		} catch (ManagementException e) {
			throw new ResourceException(Paths.SCRIPT_GET_FLOW_CHART_SERVICE, e);
		}

		String flowChartCode = MermaidConverter.buildFlowChart(scriptSource);

		return Response.status(Status.OK).entity(flowChartCode).build();
	}

}
