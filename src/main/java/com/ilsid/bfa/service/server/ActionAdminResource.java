package com.ilsid.bfa.service.server;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.manager.ActionManager;
import com.ilsid.bfa.manager.ManagementException;
import com.ilsid.bfa.service.common.Paths;
import com.sun.jersey.multipart.FormDataParam;

@Path(Paths.ACTION_SERVICE_ADMIN_ROOT)
public class ActionAdminResource extends AbstractAdminResource {

	private ActionManager actionManager;

	/**
	 * Loads meta-data items for the members of the given group. The members can be actions or/and action groups. If
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
		validateNonEmptyParameter(Paths.ACTION_GET_ITEMS_SERVICE, GROUP_PARAM, groupName);
		List<Map<String, String>> metas;
		try {
			if (groupName.equals(Metadata.ROOT_PARENT_NAME)) {
				metas = actionManager.getTopLevelActionGroupMetadatas();
			} else {
				metas = actionManager.getChildrenActionGroupMetadatas(groupName);
			}
		} catch (ManagementException e) {
			throw new ResourceException(Paths.ACTION_GET_ITEMS_SERVICE, e);
		}

		return Response.status(Status.OK).entity(metas).build();
	}

	/**
	 * Creates new action group in the code repository. The group name can be simple or complex. The simple group name
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
			actionManager.createGroup(groupName);
		} catch (ManagementException e) {
			throw new ResourceException(Paths.ACTION_CREATE_GROUP_SERVICE, e);
		}

		return Response.status(Status.OK).build();
	}

	/**
	 * 
	 * @param actionName
	 * @param actionPackage
	 * @param contentDispositionHeader
	 * @return
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path(Paths.CREATE_OPERATION)
	public Response createAction(@FormDataParam(NAME_PARAM) String actionName,
			@FormDataParam(FILE_PARAM) InputStream actionPackage) {
		validateNonEmptyParameter(Paths.CREATE_OPERATION, NAME_PARAM, actionName);
		validateNonNullParameter(Paths.CREATE_OPERATION, FILE_PARAM, actionPackage);

		try {
			actionManager.createAction(actionName, actionPackage);
		} catch (ManagementException e) {
			throw new ResourceException(Paths.ACTION_CREATE_SERVICE, e);
		}

		return Response.status(Status.OK).build();
	}

	@Inject
	public void setActionManager(ActionManager actionManager) {
		this.actionManager = actionManager;
	}

}
