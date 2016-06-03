package com.ilsid.bfa.script;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.ActionContext;
import com.ilsid.bfa.action.ActionException;
import com.ilsid.bfa.action.persistence.ActionLocator;

//TODO: complete implementation
//TODO: complete javadocs
public abstract class Script {

	private ScriptContext scriptContext;

	private Queue<Object> inputParams = new LinkedList<>();

	private Deque<String> callStack = new LinkedList<>();

	private ScriptRuntime runtime;

	private long runtimeId = -1;

	private String name;

	private ActionLocator actionLocator;

	protected abstract void doExecute() throws ScriptException;

	public Script() {
		scriptContext = new ScriptContext();
	}

	public void execute() throws ScriptException {
		doExecute();
	}

	@Var(scope = Var.Scope.INPUT)
	public void DeclareInputVar(String name, String type) throws ScriptException {
		Object value = inputParams.poll();
		if (value == null) {
			throw new ScriptException(String.format("No value was passed for the input parameter [%s]", name));
		}

		scriptContext.addInputVar(name, TypeNameResolver.resolveEntityClassName(type), value);
	}

	@Var(scope = Var.Scope.LOCAL)
	public void DeclareLocalVar(String name, String type) throws ScriptException {
		scriptContext.addLocalVar(name, TypeNameResolver.resolveEntityClassName(type));
	}

	@Var(scope = Var.Scope.LOCAL)
	public void DeclareLocalVar(String name, String type, @ExprParam Object initValue) throws ScriptException {
		scriptContext.addLocalVar(name, TypeNameResolver.resolveEntityClassName(type), getValue(initValue));
	}

	public void SetLocalVar(@ExprParam(compile = false) String name, @ExprParam Object expr) throws ScriptException {
		scriptContext.updateLocalVar(name, getValue(expr));
	}

	public Object GetGlobalVar(String name) {
		return GlobalContext.getInstance().getGlobalVar(name);
	}

	public void SetGlobalVar(String name, @ExprParam Object expr) throws ScriptException {
		GlobalContext.getInstance().setGlobalVar(name, getValue(expr));
	}

	public boolean Equal(@ExprParam Object expr1, @ExprParam Object expr2) throws ScriptException {
		return Equal(expr1, expr2, null);
	}

	public boolean Equal(@ExprParam Object expr1, @ExprParam Object expr2, String description) throws ScriptException {
		AbstractCondition condition = new EqualCondition(getValue(expr1), getValue(expr2));
		if (description != null) {
			condition.setDescription(description);
		}

		return condition.isTrue();
	}

	public boolean LessOrEqual(@ExprParam Object expr1, @ExprParam Object expr2) throws ScriptException {
		return LessOrEqual(expr1, expr2, null);
	}

	public boolean LessOrEqual(@ExprParam Object expr1, @ExprParam Object expr2, String description)
			throws ScriptException {
		AbstractCondition condition = new LessOrEqualCondition(getValue(expr1), getValue(expr2));
		if (description != null) {
			condition.setDescription(description);
		}

		return condition.isTrue();
	}

	public void SubFlow(String name) throws ScriptException {
		runtime.runScript(name, runtimeId, createSubflowCallStack());
	}

	public void SubFlow(String name, @ExprParam Object... params) throws ScriptException {
		runtime.runScript(name, toValues(params), runtimeId, createSubflowCallStack());
	}

	public ActionResult Action(String name) throws ScriptException {
		return Action(name, new Object[] {});
	}

	public ActionResult Action(String name, @ExprParam Object... params) throws ScriptException {
		Action action;
		try {
			action = actionLocator.lookup(name);
		} catch (ActionException e) {
			throw new ScriptException(String.format("Lookup of the action [%s] failed", name), e);
		}

		action.setInputParameters(toValues(params));

		Object[] result;
		try {
			result = action.execute();
		} catch (ActionException e) {
			throw new ScriptException(String.format("Execution of the action [%s] failed", name), e);
		}
		ActionResult actionResult = new ActionResultImpl(result, name);

		return actionResult;
	}

	public long getRuntimeId() {
		return runtimeId;
	}

	public void setRuntimeId(long runtimeId) {
		this.runtimeId = runtimeId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		scriptContext.setScriptName(name);
	}

	public void setActionLocator(ActionLocator actionResolver) {
		this.actionLocator = actionResolver;
	}

	public interface ActionResult {

		public ActionResult SetLocalVar(@ExprParam(compile = false) String name) throws ScriptException;

	}

	void setCallStack(Deque<String> callStack) {
		this.callStack = callStack;
	}

	void setRuntime(ScriptRuntime runtime) {
		this.runtime = runtime;
	}

	void setInputParameters(Object[] params) {
		for (Object param : params) {
			inputParams.add(param);
		}
	}

	void cleanup() {
		// Top-level flow and its sub-flows share the same action context. The context cleanup must be performed only at
		// the top-level one (after completion of all sub-flows).
		if (isTopLevel()) {
			ActionContext.cleanup();
		}
	}

	private Object[] toValues(Object[] params) throws ScriptException {
		Object[] paramValues = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			paramValues[i] = getValue(params[i]);
		}

		return paramValues;
	}

	private ValueExpression<?> toExpression(Object expr) throws ScriptException {
		if (expr instanceof ValueExpression) {
			return (ValueExpression<?>) expr;
		} else {
			try {
				return new ScriptExpression((String) expr, scriptContext);
			} catch (ClassCastException e) {
				throw new ScriptException("Expected string expression but was " + expr, e);
			}
		}
	}

	private Object getValue(Object expr) throws ScriptException {
		Object result = toExpression(expr).getValue();
		return result;
	}

	private void setLocalVarValue(String name, Object value) throws ScriptException {
		scriptContext.updateLocalVar(name, value);
	}

	private Deque<String> createSubflowCallStack() {
		// This script's name is added to the call stack of its sub-flow
		Deque<String> result = new LinkedList<>(callStack);
		result.addFirst(name);

		return result;
	}

	private boolean isTopLevel() {
		return callStack.isEmpty();
	}

	private class ActionResultImpl implements ActionResult {

		private Object[] input;

		private String actionName;

		private int size;

		private int index;

		ActionResultImpl(Object[] input, String actionName) {
			this.input = input;
			this.actionName = actionName;
			size = input.length;
		}

		public ActionResult SetLocalVar(String name) throws ScriptException {
			if (index == size) {
				throw new ScriptException(String.format(
						"No more elements found in action [%s] result. Actual result size is [%s]", actionName, size));
			}
			Script.this.setLocalVarValue(name, input[index++]);

			return this;
		}

	}

}
