package com.ilsid.bfa.service.server;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ilsid.bfa.script.ScriptException;
import com.ilsid.bfa.script.ScriptRuntime;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.RuntimeStatusType;
import com.ilsid.bfa.service.dto.ScriptRuntimeParams;
import com.ilsid.bfa.service.dto.RuntimeStatus;

/**
 * Provides the script runtime services.
 * 
 * @author illia.sydorovych
 *
 */
@Path(Paths.SCRIPT_SERVICE_RUNTIME_ROOT)
//FIXME: Handle non-default script groups
public class ScriptRuntimeResource {

	private ScriptRuntime scriptRuntime;

	/**
	 * Runs the script specified by the input parameters.
	 * 
	 * @param script
	 *            the script parameters. The script name must be specified. If the group is not specified, then the
	 *            script is searched within the Default Group.
	 * @return the response with {@link RuntimeStatus} instance including the script runtime identifier.
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the script with the specified name does not exist in the the specified group</li>
	 *             <li>if the execution of the script failed</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_RUN_OPERATION)
	// TODO: support for group name and input params is needed
	public Response run(ScriptRuntimeParams script) {
		long runtimeId;
		try {
			runtimeId = scriptRuntime.runScript(script.getName());
		} catch (ScriptException e) {
			throw new ResourceException(Paths.SCRIPT_RUN_SERVICE, e);
		}
		RuntimeStatus status = RuntimeStatus.runtimeId(runtimeId).statusType(RuntimeStatusType.COMPLETED).build();

		return Response.status(Status.OK).entity(status).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_START_OPERATION)
	public Response start(String scriptName) {
		return Response.status(Status.NOT_FOUND).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_GET_STATUS_OPERATION)
	public Response getStatus(@DefaultValue("0") @QueryParam("runtimeId") Integer runtimeId) {
		return Response.status(Status.NOT_FOUND).build();
	}

	@Inject
	public void setScriptRuntime(ScriptRuntime scriptRuntime) {
		this.scriptRuntime = scriptRuntime;
	}
}
