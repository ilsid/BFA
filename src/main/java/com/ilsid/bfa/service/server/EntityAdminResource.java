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
import com.ilsid.bfa.service.dto.EntityAdminParams;

/**
 * Provides the entity administration services.
 * 
 * @author illia.sydorovych
 *
 */

@Path(Paths.ENTITY_SERVICE_ADMIN_ROOT)
// FIXME: Handle non-default entity groups
public class EntityAdminResource extends AbstractAdminResource {

	/**
	 * Creates the entity and saves it in the code repository. If the entity's group is not specified, it is created
	 * within the Default Group.
	 * 
	 * @param entity
	 *            the entity data. The name and body must be specified
	 * @return the {@link Status#OK} response.
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the entity's name or body are not specified</li>
	 *             <li>if the entity can't be compiled or persisted</li>
	 *             <li>if the entity with the specified name already exist in the specified group</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(Paths.ENTITY_CREATE_OPERATION)
	public Response create(EntityAdminParams entity) {
		validateNonEmptyNameAndBody(Paths.ENTITY_CREATE_SERVICE, entity);
		try {
			scriptManager.createEntity(entity.getName(), entity.getBody());
		} catch (ManagementException e) {
			throw new ResourceException(Paths.ENTITY_CREATE_SERVICE, e);
		}

		return Response.status(Status.OK).build();
	}

	/**
	 * Updates the existing entity in the code repository. If the entity's group is not specified, it is searched within
	 * the Default Group.
	 * 
	 * @param entity
	 *            the entity data. The name and body must be specified
	 * @return the {@link Status#OK} response.
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the entity's name or body are not specified</li>
	 *             <li>if the entity can't be compiled or persisted</li>
	 *             <li>if the entity with the specified name does not exist in the the specified group</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(Paths.ENTITY_UPDATE_OPERATION)
	public Response update(EntityAdminParams entity) {
		validateNonEmptyNameAndBody(Paths.ENTITY_UPDATE_SERVICE, entity);
		try {
			scriptManager.updateEntity(entity.getName(), entity.getBody());
		} catch (ManagementException e) {
			throw new ResourceException(Paths.ENTITY_UPDATE_SERVICE, e);
		}

		return Response.status(Status.OK).build();
	}

	/**
	 * Loads the source code for the given entity. If the entities's group is not specified, it is searched within the
	 * Default Group.
	 * 
	 * @param entity
	 *            the entity data. The name must be specified
	 * @return the response containing the entity's source code (as plain text)
	 * @throws ResourceException
	 *             <ul>
	 *             <li>if the entity's name is not specified</li>
	 *             <li>if the entity with the specified name does not exist in the specified group</li>
	 *             <li>in case of the repository access failure</li>
	 *             </ul>
	 * @see WebApplicationExceptionMapper
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Path(Paths.ENTITY_GET_SOURCE_OPERATION)
	public Response getSource(EntityAdminParams entity) {
		validateNonEmptyName(Paths.ENTITY_GET_SOURCE_SERVICE, entity);
		String entitySource;
		try {
			entitySource = scriptManager.getEntitySourceCode(entity.getName());
		} catch (ManagementException e) {
			throw new ResourceException(Paths.ENTITY_GET_SOURCE_SERVICE, e);
		}

		return Response.status(Status.OK).entity(entitySource).build();
	}

}
