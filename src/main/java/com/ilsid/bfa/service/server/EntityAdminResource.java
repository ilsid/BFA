package com.ilsid.bfa.service.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;

import com.ilsid.bfa.common.Metadata;
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
public class EntityAdminResource extends AbstractAdminResource {

	private static final String HTTP_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	private static final String ENTITIES_JAR_NAME = "bfa-entities.jar";

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
	@Path(Paths.CREATE_OPERATION)
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
	@Path(Paths.UPDATE_OPERATION)
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
	@Path(Paths.GET_SOURCE_OPERATION)
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

	/**
	 * Loads meta-data items for the members of the given group. The members can be entities or/and entity groups. If
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
		validateNonEmptyParameter(Paths.ENTITY_GET_ITEMS_SERVICE, GROUP_PARAM, groupName);
		List<Map<String, String>> metas;
		try {
			if (groupName.equals(Metadata.ROOT_PARENT_NAME)) {
				metas = scriptManager.getTopLevelEntityGroupMetadatas();
			} else {
				metas = scriptManager.getChildrenEntityGroupMetadatas(groupName);
			}
		} catch (ManagementException e) {
			throw new ResourceException(Paths.ENTITY_GET_ITEMS_SERVICE, e);
		}

		return Response.status(Status.OK).entity(metas).build();
	}

	/**
	 * Creates new entity group in the code repository. The group name can be simple or complex. The simple group name
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
			scriptManager.createEntityGroup(groupName);
		} catch (ManagementException e) {
			throw new ResourceException(Paths.ENTITY_CREATE_GROUP_SERVICE, e);
		}

		return Response.status(Status.OK).build();
	}

	/**
	 * Downloads JAR archive with all Entity classes from the repository.
	 * 
	 * @return the response with JAR contents
	 */
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path(Paths.GET_LIBRARY_OPERATION)
	public Response getEntitiesLibrary() {
		final InputStream input;
		try {
			input = scriptManager.getEnitiesLibrary();
		} catch (ManagementException e) {
			throw new ResourceException(Paths.ENTITY_GET_LIBRARY_SERVICE, e);
		}

		StreamingOutput jarStream = new StreamingOutput() {

			public void write(OutputStream output) throws IOException, WebApplicationException {
				IOUtils.copyLarge(input, output);
				input.close();
			}

		};

		return Response.status(Status.OK)
				.header(HTTP_HEADER_CONTENT_DISPOSITION, String.format("attachment; filename=%s", ENTITIES_JAR_NAME))
				.entity(jarStream).build();
	}

}
