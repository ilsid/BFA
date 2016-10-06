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

import com.ilsid.bfa.persistence.QueryPage;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.script.ScriptException;
import com.ilsid.bfa.script.ScriptRuntime;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.RuntimeStatus;
import com.ilsid.bfa.service.dto.ScriptRuntimeParams;
import com.ilsid.bfa.service.dto.ScriptRuntimeQuery;

/**
 * Provides the script runtime services.
 * 
 * @author illia.sydorovych
 *
 */
@Path(Paths.SCRIPT_SERVICE_RUNTIME_ROOT)
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
	@Path(Paths.RUN_OPERATION)
	public Response run(ScriptRuntimeParams script) {
		Object runtimeId;
		try {
			final Object[] params = script.getInputParameters();
			if (params != null && params.length > 0) {
				runtimeId = scriptRuntime.runScript(script.getName(), params);
			} else {
				runtimeId = scriptRuntime.runScript(script.getName());
			}
		} catch (ScriptException e) {
			throw new ResourceException(Paths.SCRIPT_RUN_SERVICE, e);
		}
		RuntimeStatus status = RuntimeStatus.runtimeId(runtimeId).statusType(RuntimeStatusType.COMPLETED).build();

		return Response.status(Status.OK).entity(status).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.START_OPERATION)
	public Response start(String scriptName) {
		return Response.status(Status.NOT_FOUND).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.SCRIPT_GET_STATUS_OPERATION)
	public Response getStatus(@DefaultValue("0") @QueryParam("runtimeId") Integer runtimeId) {
		return Response.status(Status.NOT_FOUND).build();
	}

	/**
	 * Fetches scripting runtime records by the given query. The result is paginated.
	 * 
	 * @param query
	 *            query to execute
	 * @return response with {@link QueryPage} entity
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the query contains a page token and this token is invalid</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path(Paths.FETCH_OPERATION)
	public Response fetch(ScriptRuntimeQuery query) {
		QueryPage<ScriptRuntimeDTO> result;
		final int fetchLimit = query.getResultsPerPage();
		final String pageToken = query.getPageToken();
		final ScriptRuntimeCriteria criteria = query.getCriteria();

		try {
			if (pageToken != null) {
				result = scriptRuntime.fetch(criteria, fetchLimit, pageToken);
			} else {
				result = scriptRuntime.fetch(criteria, fetchLimit);
			}
		} catch (ScriptException e) {
			throw new ResourceException(Paths.SCRIPT_RUNTIME_FETCH_SERVICE, e);
		}

		return Response.status(Status.OK).entity(result).build();
	}

	@Inject
	public void setScriptRuntime(ScriptRuntime scriptRuntime) {
		this.scriptRuntime = scriptRuntime;
	}

}
