package com.ilsid.bfa.action;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;

/**
 * Action context.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class ActionContext {

	private static ThreadLocal<ActionContext> contextHolder = new ThreadLocal<ActionContext>() {

		@Override
		protected ActionContext initialValue() {
			return new ActionContextImpl();
		};
	};

	/**
	 * Gets parameter's value from the context.
	 * 
	 * @param name
	 *            parameter name
	 * @return parameter's value or <code>null</code>, if such parameter does not exist
	 * @throws IllegalArgumentException
	 *             if the passed name is <code>null</code>
	 */
	public abstract Object getParameter(String name);

	/**
	 * Adds parameter to the context.
	 * 
	 * @param name
	 *            parameter's name
	 * @param value
	 *            parameter's value
	 * @throws IllegalArgumentException
	 *             if the passed name or value is <code>null</code>
	 */
	public abstract void putParameter(String name, Object value);

	/**
	 * Returns instance for the current thread. Creates new instance on the first invocation or if
	 * {@linkplain #cleanup()} has been invoked right before. Returns the same instance on the consequent invocations.
	 * 
	 * @return thread local instance
	 * @see #cleanup()
	 */
	public static ActionContext getInstance() {
		return contextHolder.get();
	}

	/**
	 * Removes the context instance for the current thread. Next invocation of {@linkplain #getInstance()} returns new
	 * instance.
	 * 
	 * @see #getInstance()
	 */
	public static void cleanup() {
		contextHolder.remove();
	}

	private static class ActionContextImpl extends ActionContext {

		private static final String NAME_IS_NULL_ERR_MSG = "Parameter's name must not be null";

		private static final String VALUE_IS_NULL_ERR_MSG = "Parameter's value must not be null";

		private Map<String, Object> params = new HashMap<>();

		@Override
		public Object getParameter(String name) {
			Validate.notNull(name, NAME_IS_NULL_ERR_MSG);
			return params.get(name);
		}

		@Override
		public void putParameter(String name, Object value) {
			Validate.notNull(name, NAME_IS_NULL_ERR_MSG);
			Validate.notNull(value, VALUE_IS_NULL_ERR_MSG);
			params.put(name, value);
		}

	}

}
