package com.ilsid.bfa.script;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.action.persistence.ActionLocator;
import com.ilsid.bfa.common.ConfigUtil;
import com.ilsid.bfa.common.ExceptionUtil;
import com.ilsid.bfa.common.LoggingConfig;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.QueryPage;
import com.ilsid.bfa.persistence.QueryPagingOptions;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.runtime.monitor.MonitoringException;
import com.ilsid.bfa.runtime.monitor.MonitoringServer;
import com.ilsid.bfa.runtime.monitor.MonitoringServerConfig;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

/**
 * Provides the script runtime operations.
 * 
 * @author illia.sydorovych
 *
 */
@Singleton
public class ScriptRuntime {

	private static final String RUNTIME_DEBUG_LOGGING_FLAG = "bfa.logging.runtime_debug";

	private static final String CONFIG_PROP_MONITOR_SERVER_HOST = "bfa.monitor.server.host";

	private static final String CONFIG_PROP_MONITOR_SERVER_PORT = "bfa.monitor.server.port";

	private static final String MONITORING_SERVER_URL_PREFIX = "ws://";

	private static final int MONITOR_SERVER_PORT_DEFAULT_VALUE = 8025;

	// FIXME: introduce authentication
	private static final String STUBBED_USER_NAME = "system";

	private static final Object[] EMPTY_PARAMS = new Object[] {};

	private final Logger runtimeLogger = LoggerFactory.getLogger("runtime_debug_logger");

	private RuntimeRepository repository;

	private ActionLocator actionLocator;

	private boolean runtimeDebugEnabled;

	private String monitoringServerURL;

	/**
	 * Runs the script with the given name.
	 * 
	 * @param scriptName
	 *            the script name
	 * @return the script runtime identifier
	 * @throws ScriptException
	 *             <ul>
	 *             <li>if the script with such name does not exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             <li>in case of the script runtime failure</li>
	 *             </ul>
	 */
	public Object runScript(String scriptName) throws ScriptException {
		return runScript(scriptName, EMPTY_PARAMS, null, null);
	}

	/**
	 * Runs the script with the given name and input parameters.
	 * 
	 * @param scriptName
	 *            the script name
	 * @param params
	 *            input parameters
	 * @return the script runtime identifier
	 * @throws ScriptException
	 *             <ul>
	 *             <li>if the script with such name does not exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             <li>in case of the script runtime failure</li>
	 *             </ul>
	 */
	public Object runScript(String scriptName, Object[] params) throws ScriptException {
		return runScript(scriptName, params, null, null);
	}

	/**
	 * Fetches script runtime info by the given criteria. If the result exceeds the fetch limit, only the first query
	 * page is returned.
	 * 
	 * @param criteria
	 *            fetch criteria
	 * @param resultsPerPage
	 *            fetch records limit
	 * @return all records or the first page only, if the result exceeds the fetch limit
	 * @throws ScriptException
	 *             in case of any repository access issues
	 */
	public QueryPage<ScriptRuntimeDTO> fetch(ScriptRuntimeCriteria criteria, int resultsPerPage)
			throws ScriptException {
		return fetch(criteria, resultsPerPage, null);
	}

	/**
	 * Does the same as {@link #fetch(ScriptRuntimeCriteria, int)}, but explicitly defines the token of a page to
	 * return.
	 * 
	 * @param criteria
	 *            fetch criteria
	 * @param resultsPerPage
	 *            maximum number of records per one query page
	 * @param pageToken
	 *            page token
	 * @return the defined page of the fetch result
	 * @throws ScriptException
	 *             <ul>
	 *             <li>invalid page token is provided</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public QueryPage<ScriptRuntimeDTO> fetch(ScriptRuntimeCriteria criteria, int resultsPerPage, String pageToken)
			throws ScriptException {
		QueryPagingOptions pagingOptions = new QueryPagingOptions();
		pagingOptions.setResultsPerPage(resultsPerPage);

		if (pageToken != null) {
			pagingOptions.setPageToken(pageToken);
		}

		QueryPage<ScriptRuntimeDTO> result;
		try {
			result = repository.fetch(criteria, pagingOptions);
		} catch (PersistenceException e) {
			throw new ScriptException("Failed to fetch script runtime info", e);
		}

		return result;
	}

	/**
	 * Defines a code repository implementation.
	 * 
	 * @param repository
	 *            a code repository
	 */
	@Inject
	public void setRepository(RuntimeRepository repository) {
		this.repository = repository;
	}

	/**
	 * Defines the action locator.
	 * 
	 * @param actionLocator
	 *            action locator instance
	 */
	@Inject
	public void setActionLocator(ActionLocator actionLocator) {
		this.actionLocator = actionLocator;
	}

	/**
	 * Defines a logging configuration.
	 * 
	 * @param loggingConfig
	 *            logging configuration
	 */
	@Inject
	public void setLoggingConfig(@LoggingConfig Map<String, String> loggingConfig) {
		final String configValue = loggingConfig.get(RUNTIME_DEBUG_LOGGING_FLAG);
		// Config property has more priority over system property
		if (configValue != null) {
			runtimeDebugEnabled = Boolean.parseBoolean(configValue);
		} else {
			runtimeDebugEnabled = Boolean.getBoolean(RUNTIME_DEBUG_LOGGING_FLAG);
		}
	}

	/**
	 * Initializes and starts the monitoring server.
	 * 
	 * @param serverConfig
	 *            server configuration
	 * @throws ConfigurationException
	 *             if the server can't be started
	 */
	@Inject
	public void initMonitoringServer(@MonitoringServerConfig Map<String, String> serverConfig)
			throws ConfigurationException {
		String host;
		int port;
		try {
			host = serverConfig.get(CONFIG_PROP_MONITOR_SERVER_HOST);
			if (host == null) {
				host = Inet4Address.getLocalHost().getCanonicalHostName();
			}
			port = ConfigUtil.getPositiveIntegerValue(CONFIG_PROP_MONITOR_SERVER_PORT, serverConfig,
					MONITOR_SERVER_PORT_DEFAULT_VALUE);

			MonitoringServer.start(host, port);
		} catch (UnknownHostException | MonitoringException e) {
			throw new IllegalStateException("Failed to start the monitoring server", e);
		}

		monitoringServerURL = MONITORING_SERVER_URL_PREFIX + host + ":" + port + MonitoringServer.CONTEXT_PATH
				+ MonitoringServer.MONITOR_END_POINT;
	}

	/**
	 * Returns the monitoring server URL. This method must be invoked only after {@link #initMonitoringServer(Map)},
	 * otherwise <code>null</code> is returned.
	 * 
	 * @return the monitoring server URL
	 * @see #initMonitoringServer(Map)
	 */
	public String getMonitoringServerURL() {
		return monitoringServerURL;
	}

	Object runScript(String scriptName, Object runtimeId, Deque<String> callStack) throws ScriptException {
		return runScript(scriptName, EMPTY_PARAMS, runtimeId, callStack);
	}

	Object runScript(String scriptName, Object[] params, Object runtimeId, Deque<String> callStack)
			throws ScriptException {
		Script script = createInstance(scriptName);

		Object flowRuntimeId;
		if (runtimeId == null) {
			flowRuntimeId = generatedRuntimeId(scriptName);
		} else {
			flowRuntimeId = runtimeId;
		}

		script.setRuntimeId(flowRuntimeId);
		script.setName(scriptName);
		script.setRuntime(this);
		script.setActionLocator(actionLocator);
		script.setInputParameters(params);
		if (callStack != null) {
			script.setCallStack(callStack);
		}

		if (runtimeDebugEnabled) {
			script.setRuntimeLogger(createRuntimeLogger(flowRuntimeId, scriptName, callStack));
		}

		ScriptRuntimeDTO runtimeRecord = new ScriptRuntimeDTO().setRuntimeId(flowRuntimeId)
				.setUserName(STUBBED_USER_NAME).setScriptName(scriptName).setParameters(toStrings(params))
				.setStatus(RuntimeStatusType.INPROGRESS).setStartTime(new Date()).setCallStack(callStack);

		createRuntimeRecord(runtimeRecord);
		try {
			script.execute();
		} catch (ScriptException e) {
			updateRuntimeRecord(addErrorInfo(runtimeRecord, e));
			throw e;
		} catch (RuntimeException e) {
			updateRuntimeRecord(addErrorInfo(runtimeRecord, e));
			throw new ScriptException(String.format("Script [%s] failed", scriptName), e);
		} catch (Error e) {
			updateRuntimeRecord(addErrorInfo(runtimeRecord, new Exception("System error occurred", e)));
			throw new ScriptException(String.format("Script [%s] failed with system error", scriptName), e);
		} finally {
			script.cleanup();
		}

		updateRuntimeRecord(runtimeRecord.setStatus(RuntimeStatusType.COMPLETED).setEndTime(new Date()));

		return flowRuntimeId;
	}

	@SuppressWarnings("unchecked")
	private Script createInstance(String scriptName) throws ScriptException {
		String scriptClassName = TypeNameResolver.resolveScriptClassName(scriptName);
		Class<Script> scriptClass;
		try {
			scriptClass = (Class<Script>) DynamicClassLoader.getInstance().loadClass(scriptClassName);
		} catch (ClassNotFoundException e) {
			throw new ScriptException(String.format("The script [%s] is not found in the repository", scriptName), e);
		} catch (ClassCastException e) {
			throw new ScriptException(
					"Unexpected class was loaded from the repository. Expected: " + Script.class.getName(), e);
		}

		Script script;
		try {
			script = scriptClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ScriptException(String.format("Failed to create an instance of the script [%s]", scriptName), e);
		}

		return script;
	}

	private ScriptRuntimeDTO addErrorInfo(ScriptRuntimeDTO runtimeRecord, Exception e) {
		final List<String> errorDetails = Arrays
				.asList(ExceptionUtil.getExceptionMessageChain(e).split(StringUtils.LF));

		return runtimeRecord.setStatus(RuntimeStatusType.FAILED).setEndTime(new Date()).setErrorDetails(errorDetails);
	}

	private Object generatedRuntimeId(String scriptName) throws ScriptException {
		Object runtimeId;
		try {
			runtimeId = repository.getNextRuntimeId();
		} catch (PersistenceException e) {
			throw new ScriptException(String.format("Failed to generate runtime id for the script [%s]", scriptName),
					e);
		}

		return runtimeId;
	}

	private void createRuntimeRecord(ScriptRuntimeDTO record) throws ScriptException {
		try {
			repository.createRuntimeRecord(record);
		} catch (PersistenceException e) {
			throw new ScriptException(
					String.format("Failed to create runtime record for the script [%s]", record.getScriptName()), e);
		}

	}

	private void updateRuntimeRecord(ScriptRuntimeDTO record) throws ScriptException {
		try {
			repository.updateRuntimeRecord(record);
		} catch (PersistenceException e) {
			throw new ScriptException(
					String.format("Failed to update runtime record for the script [%s]", record.getScriptName()), e);
		}

	}

	private RuntimeLogger createRuntimeLogger(Object flowRuntimeId, String scriptName, Deque<String> callStack) {
		StringBuilder logPrefix = new StringBuilder().append("Script ");
		logPrefix.append("[runtimeId=").append(flowRuntimeId).append("] [");

		if (callStack != null && callStack.size() > 0) {
			List<String> parentNames = new ArrayList<>(callStack);
			Collections.reverse(parentNames);
			for (String parName : parentNames) {
				logPrefix.append(parName).append("-->");
			}
		}
		logPrefix.append(scriptName).append("]");

		RuntimeLogger logger = new RuntimeLogger(runtimeLogger, logPrefix.toString());

		return logger;
	}

	private List<String> toStrings(Object[] params) {
		List<String> result = new LinkedList<>();
		for (Object param : params) {
			result.add(param.toString());
		}

		return result;
	}
}
