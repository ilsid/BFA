package com.ilsid.bfa.service.common;

/**
 * The holder for the service URI paths
 * 
 * @author illia.sydorovych
 *
 */
public final class Paths {

	public final static String SERVICE_ROOT = "service";

	public final static String SCRIPT_SERVICE_ROOT = SERVICE_ROOT + "/script";

	public final static String ENTITY_SERVICE_ROOT = SERVICE_ROOT + "/entity";
	
	public final static String ACTION_SERVICE_ROOT = SERVICE_ROOT + "/action";

	/*
	 * Common Paths
	 */

	public final static String CREATE_OPERATION = "create";

	public final static String UPDATE_OPERATION = "update";

	public final static String GET_SOURCE_OPERATION = "getSource";

	public final static String CREATE_GROUP_OPERATION = "createGroup";

	public final static String GET_ITEMS_OPERATION = "getItems";

	/*
	 * Script Administration
	 */

	public final static String SCRIPT_SERVICE_ADMIN_ROOT = SCRIPT_SERVICE_ROOT + "/admin";

	public final static String SCRIPT_CREATE_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + CREATE_OPERATION;

	public final static String SCRIPT_UPDATE_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + UPDATE_OPERATION;

	public final static String SCRIPT_GET_SOURCE_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + GET_SOURCE_OPERATION;

	public final static String SCRIPT_GET_ITEMS_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + GET_ITEMS_OPERATION;

	public final static String SCRIPT_CREATE_GROUP_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + CREATE_GROUP_OPERATION;

	/*
	 * Script Runtime
	 */

	public final static String SCRIPT_SERVICE_RUNTIME_ROOT = SCRIPT_SERVICE_ROOT + "/runtime";

	public final static String SCRIPT_RUN_OPERATION = "run";

	public final static String SCRIPT_START_OPERATION = "start";

	public final static String SCRIPT_GET_STATUS_OPERATION = "getStatus";

	public final static String SCRIPT_RUN_SERVICE = SCRIPT_SERVICE_RUNTIME_ROOT + "/" + SCRIPT_RUN_OPERATION;

	public final static String SCRIPT_START_SERVICE = SCRIPT_SERVICE_RUNTIME_ROOT + "/" + SCRIPT_START_OPERATION;

	public final static String SCRIPT_GET_STATUS_SERVICE = SCRIPT_SERVICE_RUNTIME_ROOT + "/"
			+ SCRIPT_GET_STATUS_OPERATION;

	/*
	 * Entity Administration
	 */

	public final static String ENTITY_SERVICE_ADMIN_ROOT = ENTITY_SERVICE_ROOT + "/admin";

	public final static String ENTITY_CREATE_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + CREATE_OPERATION;

	public final static String ENTITY_UPDATE_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + UPDATE_OPERATION;

	public final static String ENTITY_GET_SOURCE_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + GET_SOURCE_OPERATION;

	public final static String ENTITY_GET_ITEMS_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + GET_ITEMS_OPERATION;

	public final static String ENTITY_CREATE_GROUP_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + CREATE_GROUP_OPERATION;
	
	/*
	 * Action Administration
	 */
	
	public final static String ACTION_SERVICE_ADMIN_ROOT = ACTION_SERVICE_ROOT + "/admin";
	
	public final static String ACTION_CREATE_GROUP_SERVICE = ACTION_SERVICE_ADMIN_ROOT + "/" + CREATE_GROUP_OPERATION;
	
	public final static String ACTION_GET_ITEMS_SERVICE = ACTION_SERVICE_ADMIN_ROOT + "/" + GET_ITEMS_OPERATION;
}
