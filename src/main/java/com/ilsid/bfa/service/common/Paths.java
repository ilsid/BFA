package com.ilsid.bfa.service.common;

/**
 * The holder for relative URI paths
 * 
 * @author illia.sydorovych
 *
 */
public final class Paths {

	public final static String SCRIPT_SERVICE_ROOT = "script";
	
	public final static String SCRIPT_SERVICE_ADMIN_ROOT = SCRIPT_SERVICE_ROOT + "/admin";
	
	public final static String SCRIPT_SERVICE_RUNTIME_ROOT = SCRIPT_SERVICE_ROOT + "/runtime";

	public final static String SCRIPT_CREATE_OPERATION = "create";

	public final static String SCRIPT_UPDATE_OPERATION = "update";

	public final static String SCRIPT_GET_SOURCE_OPERATION = "getSource";

	public final static String SCRIPT_CREATE_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + SCRIPT_CREATE_OPERATION;

	public final static String SCRIPT_UPDATE_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + SCRIPT_UPDATE_OPERATION;

	public final static String SCRIPT_GET_SOURCE_SERVICE = SCRIPT_SERVICE_ADMIN_ROOT + "/" + SCRIPT_GET_SOURCE_OPERATION;

}
