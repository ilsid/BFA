package com.ilsid.bfa.script;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.ActionException;
import com.ilsid.bfa.action.persistence.ActionLocator;

//TODO: complete implementation
//TODO: complete javadocs
public abstract class Script {

	private ScriptContext scriptContext;

	private GlobalContext runtimeContext;

	private ScriptRuntime runtime;

	private long runtimeId = -1;

	private ActionLocator actionLocator;

	protected abstract void doExecute() throws ScriptException;

	public Script() {
		scriptContext = new ScriptContext(GlobalContext.getInstance());
		scriptContext.setScriptName(this.getClass().getSimpleName());
	}

	public void execute() throws ScriptException {
		doExecute();
	}

	public void setRuntimeContext(GlobalContext runtimeContext) {
		this.runtimeContext = runtimeContext;
		scriptContext = new ScriptContext(runtimeContext);
		scriptContext.setScriptName(this.getClass().getSimpleName());
	}

	@Var(scope = Var.Scope.INPUT)
	public void DeclareInputVar(String name, String type) throws ScriptException {
		scriptContext.addInputVar(name, TypeNameResolver.resolveEntityClassName(type));
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
		return runtimeContext.getGlobalVar(name);
	}

	public void SetGlobalVar(String name, @ExprParam Object expr) throws ScriptException {
		runtimeContext.setGlobalVar(name, getValue(expr));
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
		// TODO: pass input parameters
		// TODO: maybe some parent info is needed
		runtime.runScript(name);
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
		
		Object[] paramValues = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			paramValues[i] = getValue(params[i]);
		}

		action.setInputParameters(paramValues);

		Object[] result;
		try {
			result = action.execute();
		} catch (ActionException e) {
			throw new ScriptException(String.format("Execution of the action [%s] failed", name), e);
		}
		ActionResult actionResult = new ActionResultImpl(result);

		return actionResult;
	}

	public ValueExpression<Boolean> AsBoolean(String input) {
		return new BooleanExpression(input);
	}

	public ValueExpression<String> AsString(String input) {
		return new StringLiteralExpression(input);
	}

	public long getRuntimeId() {
		return runtimeId;
	}

	public void setRuntimeId(long runtimeId) {
		this.runtimeId = runtimeId;
	}

	public void setActionLocator(ActionLocator actionResolver) {
		this.actionLocator = actionResolver;
	}

	public interface ActionResult {

		public ActionResult SetLocalVar(@ExprParam(compile = false) String name) throws ScriptException;

	}

	void setRuntime(ScriptRuntime runtime) {
		this.runtime = runtime;
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

	private class ActionResultImpl implements ActionResult {

		private Object[] input;

		private int size;

		private int index;

		ActionResultImpl(Object[] input) {
			this.input = input;
			size = input.length;
		}

		public ActionResult SetLocalVar(String name) throws ScriptException {
			if (index == size) {
				throw new ScriptException("No more elements found in action result");
			}
			Script.this.setLocalVarValue(name, input[index++]);

			return this;
		}

	}

}
