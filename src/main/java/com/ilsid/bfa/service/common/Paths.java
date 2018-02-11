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

	public final static String CREATE_QUIETLY_OPERATION = "createQuietly";

	public final static String UPDATE_OPERATION = "update";

	public final static String UPDATE_QUIETLY_OPERATION = "updateQuietly";
	
	public final static String DELETE_OPERATION = "delete";

	public final static String GET_SOURCE_OPERATION = "getSource";
	
	public final static String CREATE_GROUP_OPERATION = "createGroup";

	public final static String GET_ITEMS_OPERATION = "getItems";

	public final static String GET_INFO_OPERATION = "getInfo";

	public final static String FETCH_OPERATION = "fetch";

	/*
	 * Script Administration
	 */

	public final static String GET_INPUT_PARAMS_OPERATION = "getInputParameters";

	public final static String GET_FLOW_CHART_OPERATION = "getFlowChart";
	
	public final static String GET_DIAGRAM_OPERATION = "getDiagram";

	public final static String SCRIPT_SERVICE_ADMIN_ROOT = SCRIPT_SERVICE_ROOT + "/admin";

	public final static String SCRIPT_CREATE_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + CREATE_OPERATION;

	public final static String SCRIPT_UPDATE_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + UPDATE_OPERATION;
	
	public final static String SCRIPT_DELETE_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + DELETE_OPERATION;

	public final static String SCRIPT_GET_SOURCE_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + GET_SOURCE_OPERATION;
	
	public final static String SCRIPT_GET_DIAGRAM_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + GET_DIAGRAM_OPERATION;

	public final static String SCRIPT_GET_ITEMS_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + GET_ITEMS_OPERATION;

	public final static String SCRIPT_CREATE_GROUP_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + CREATE_GROUP_OPERATION;

	public final static String SCRIPT_GET_INPUT_PARAMS_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/"
			+ GET_INPUT_PARAMS_OPERATION;

	public final static String SCRIPT_GET_FLOW_CHART_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/"
			+ GET_FLOW_CHART_OPERATION;

	/*
	 * Script Runtime
	 */

	public final static String SCRIPT_SERVICE_RUNTIME_ROOT = SCRIPT_SERVICE_ROOT + "/runtime";

	public final static String RUN_OPERATION = "run";

	public final static String START_OPERATION = "start";

	public final static String SCRIPT_GET_STATUS_OPERATION = "getStatus";

	public final static String GET_MONITORING_SERVER_URL_OPERATION = "getMonitoringServerUrl";

	public final static String SCRIPT_RUN_SERVICE = SCRIPT_SERVICE_RUNTIME_ROOT + "/" + RUN_OPERATION;

	public final static String SCRIPT_START_SERVICE = SCRIPT_SERVICE_RUNTIME_ROOT + "/" + START_OPERATION;

	public final static String SCRIPT_GET_STATUS_SERVICE = SCRIPT_SERVICE_RUNTIME_ROOT + "/"
			+ SCRIPT_GET_STATUS_OPERATION;

	public final static String SCRIPT_RUNTIME_FETCH_SERVICE = SCRIPT_SERVICE_RUNTIME_ROOT + "/" + FETCH_OPERATION;

	public final static String SCRIPT_GET_RUNTIME_MONITORING_SERVER_URL_SERVICE = SCRIPT_SERVICE_RUNTIME_ROOT + "/"
			+ GET_MONITORING_SERVER_URL_OPERATION;

	/*
	 * Entity Administration
	 */

	public final static String ENTITY_SERVICE_ADMIN_ROOT = ENTITY_SERVICE_ROOT + "/admin";

	public final static String GET_LIBRARY_OPERATION = "getLibrary";

	public final static String ENTITY_CREATE_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + CREATE_OPERATION;

	public final static String ENTITY_UPDATE_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + UPDATE_OPERATION;
	
	public final static String ENTITY_DELETE_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + DELETE_OPERATION;

	public final static String ENTITY_GET_SOURCE_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + GET_SOURCE_OPERATION;

	public final static String ENTITY_GET_ITEMS_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + GET_ITEMS_OPERATION;

	public final static String ENTITY_CREATE_GROUP_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + CREATE_GROUP_OPERATION;

	public final static String ENTITY_GET_LIBRARY_SERVICE = ENTITY_SERVICE_ADMIN_ROOT + "/" + GET_LIBRARY_OPERATION;

	/*
	 * Action Administration
	 */

	public final static String ACTION_SERVICE_ADMIN_ROOT = ACTION_SERVICE_ROOT + "/admin";

	public final static String ACTION_CREATE_GROUP_SERVICE = ACTION_SERVICE_ADMIN_ROOT + "/" + CREATE_GROUP_OPERATION;

	public final static String ACTION_GET_ITEMS_SERVICE = ACTION_SERVICE_ADMIN_ROOT + "/" + GET_ITEMS_OPERATION;

	public final static String ACTION_CREATE_SERVICE = ACTION_SERVICE_ADMIN_ROOT + "/" + CREATE_OPERATION;

	public final static String ACTION_UPDATE_SERVICE = ACTION_SERVICE_ADMIN_ROOT + "/" + UPDATE_OPERATION;
	
	public final static String ACTION_DELETE_SERVICE = ACTION_SERVICE_ADMIN_ROOT + "/" + DELETE_OPERATION;

	public final static String ACTION_CREATE_QUIETLY_SERVICE = ACTION_SERVICE_ADMIN_ROOT + "/"
			+ CREATE_QUIETLY_OPERATION;

	public final static String ACTION_GET_INFO_SERVICE = ACTION_SERVICE_ADMIN_ROOT + "/" + GET_INFO_OPERATION;

	public final static String ACTION_UPDATE_QUIETLY_SERVICE = ACTION_SERVICE_ADMIN_ROOT + "/"
			+ UPDATE_QUIETLY_OPERATION;
}
