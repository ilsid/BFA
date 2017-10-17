package com.ilsid.bfa.script;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.ActionContext;
import com.ilsid.bfa.action.ActionException;
import com.ilsid.bfa.action.persistence.ActionLocator;
import com.ilsid.bfa.flow.FlowElement;
import com.ilsid.bfa.flow.FlowConstants;

//TODO: complete implementation
//TODO: complete javadocs
//TODO: refactor logging
public abstract class Script {

	protected ScriptContext scriptContext;

	private Queue<Object> inputParams = new LinkedList<>();

	private Deque<String> callStack = new LinkedList<>();

	private ScriptRuntime runtime;

	private Object runtimeId = -1;

	private String name;

	private ActionLocator actionLocator;

	private RuntimeLogger runtimeLogger;

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
		scriptContext.addLocalVar(name, TypeNameResolver.resolveEntityClassName(type), initValue);
	}

	@FlowElement(type = FlowConstants.OPERATION, description = "Set %0 = %1")
	public void SetLocalVar(@ExprParam(replaceOnCompile = false, type = ExprParam.Type.VAR_OR_FLD_NAME) String name,
			@ExprParam Object expr) throws ScriptException {
		scriptContext.updateLocalVar(name, expr);
	}
	
	public Object GetGlobalVar(String name) {
		return GlobalContext.getInstance().getGlobalVar(name);
	}

	public void SetGlobalVar(String name, @ExprParam Object expr) throws ScriptException {
		GlobalContext.getInstance().setGlobalVar(name, expr);
	}

	@FlowElement(type = FlowConstants.CONDITION, description = "%0 == %1")
	public boolean Equal(@ExprParam Object expr1, @ExprParam Object expr2) throws ScriptException {
		return Equal(expr1, expr2, null);
	}

	@FlowElement(type = FlowConstants.CONDITION, description = "%2")
	public boolean Equal(@ExprParam Object expr1, @ExprParam Object expr2, String description) throws ScriptException {
		if (runtimeLogger != null) {
			runtimeLogger.debug(new StringBuilder("Equal: ").append(expr1).append(", ").append(expr2).toString());
		}

		AbstractCondition condition = new EqualCondition(expr1, expr2);
		if (description != null) {
			condition.setDescription(description);
		}

		return condition.isTrue();
	}

	@FlowElement(type = FlowConstants.CONDITION, description = "%0 <= %1")
	public boolean LessOrEqual(@ExprParam Object expr1, @ExprParam Object expr2) throws ScriptException {
		return LessOrEqual(expr1, expr2, null);
	}

	@FlowElement(type = FlowConstants.CONDITION, description = "%2")
	public boolean LessOrEqual(@ExprParam Object expr1, @ExprParam Object expr2, String description)
			throws ScriptException {
		if (runtimeLogger != null) {
			runtimeLogger.debug(new StringBuilder("LessOrEqual: ").append(expr1).append(", ").append(expr2).toString());
		}

		AbstractCondition condition = new LessOrEqualCondition(expr1, expr2);
		if (description != null) {
			condition.setDescription(description);
		}

		return condition.isTrue();
	}

	@FlowElement(type = FlowConstants.SUBFLOW, description = "%0")
	public void SubFlow(String name) throws ScriptException {
		if (runtimeLogger != null) {
			runtimeLogger.debug("SubFlow: ".concat(name));
		}

		runtime.runScript(name, runtimeId, createSubflowCallStack());
	}

	@FlowElement(type = FlowConstants.SUBFLOW, description = "%0")
	public void SubFlow(String name, @ExprParam Object... params) throws ScriptException {
		if (runtimeLogger != null) {
			runtimeLogger.debug(new StringBuilder("SubFlow: ").append(name).append(", parameters: ")
					.append(Arrays.toString(params)).toString());
		}

		runtime.runScript(name, params, runtimeId, createSubflowCallStack());
	}

	@FlowElement(type = FlowConstants.OPERATION, description = "%0")
	public ActionResult Action(String name) throws ScriptException {
		return Action(name, new Object[] {});
	}

	@FlowElement(type = FlowConstants.OPERATION, description = "%0")
	public ActionResult Action(String name, @ExprParam Object... params) throws ScriptException {
		if (runtimeLogger != null) {
			runtimeLogger.debug(new StringBuilder("Action: ").append(name).append(", parameters: ")
					.append(Arrays.toString(params)).toString());
		}

		Action action;
		try {
			action = actionLocator.lookup(name);
		} catch (ActionException e) {
			throw new ScriptException(String.format("Lookup of the action [%s] failed", name), e);
		}

		action.setInputParameters(params);

		Object[] result;
		try {
			result = action.execute();
		} catch (ActionException e) {
			throw new ScriptException(String.format("Execution of the action [%s] failed", name), e);
		}

		if (result == null) {
			result = new Object[] {};
		}
		ActionResult actionResult = new ActionResultImpl(result, name);

		return actionResult;
	}

	public Object getRuntimeId() {
		return runtimeId;
	}

	public void setRuntimeId(Object runtimeId) {
		this.runtimeId = runtimeId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		scriptContext.setScriptName(name);
	}

	public void setActionLocator(ActionLocator actionLocator) {
		this.actionLocator = actionLocator;
	}

	public interface ActionResult {

		@FlowElement(type = FlowConstants.OPERATION, description = "Assign %0")
		public ActionResult SetLocalVar(
				@ExprParam(replaceOnCompile = false, type = ExprParam.Type.VAR_OR_FLD_NAME) String name)
				throws ScriptException;

		@FlowElement(type = FlowConstants.OPERATION, description = "Assign %0")
		public void SetResult(@ExprParam(replaceOnCompile = false, type = ExprParam.Type.VAR_NAME) String name)
				throws ScriptException;

	}

	void setCallStack(Deque<String> callStack) {
		this.callStack = callStack;
	}

	int getCallStackSize() {
		return callStack.size();
	}

	void setRuntime(ScriptRuntime runtime) {
		this.runtime = runtime;
	}

	void setInputParameters(Object[] params) {
		for (Object param : params) {
			inputParams.add(param);
		}
	}

	void setRuntimeLogger(RuntimeLogger runtimeLogger) {
		this.runtimeLogger = runtimeLogger;
	}

	void cleanup() {
		// Top-level flow and its sub-flows share the same action context. The context cleanup must be performed only at
		// the top-level one (after completion of all sub-flows).
		if (isTopLevel()) {
			ActionContext.cleanup();
		}
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

		public void SetResult(String name) throws ScriptException {
			Script.this.scriptContext.updateLocalVar(name, input);
		}

	}

}
