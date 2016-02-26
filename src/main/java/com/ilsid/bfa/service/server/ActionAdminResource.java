package com.ilsid.bfa.service.server;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ilsid.bfa.manager.ActionManager;
import com.ilsid.bfa.manager.ManagementException;
import com.ilsid.bfa.service.common.Paths;

@Path(Paths.ACTION_SERVICE_ADMIN_ROOT)
public class ActionAdminResource extends AbstractAdminResource {

	private ActionManager actionManager;

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
		validateNonEmptyParameter(Paths.CREATE_GROUP_OPERATION, GROUP_PARAM_NAME, groupName);

		try {
			actionManager.createGroup(groupName);
		} catch (ManagementException e) {
			throw new ResourceException(Paths.ACTION_CREATE_GROUP_SERVICE, e);
		}

		return Response.status(Status.OK).build();
	}

	@Inject
	public void setActionManager(ActionManager actionManager) {
		this.actionManager = actionManager;
	}

}
