package com.ilsid.bfa.script;

import com.ilsid.bfa.runtime.RuntimeContext;

//TODO: complete implementation
//TODO: complete javadocs
public abstract class Script implements Executable<Void> {

	private ScriptContext scriptContext;
	
	private RuntimeContext runtimeContext;
	
	private TypeResolver typeResolver; 

	protected abstract void doExecute() throws ScriptException;

	public Void execute() throws ScriptException {
		doExecute();
		return null;
	}
	
	public void setRuntimeContext(RuntimeContext runtimeContext) {
		this.runtimeContext = runtimeContext;
		scriptContext = new ScriptContext(runtimeContext);
		scriptContext.setScriptName(this.getClass().getSimpleName());
	}
	
	public void DeclareInputVar(String name, String type) throws ScriptException {
		scriptContext.addInputVar(name, typeResolver.resolveJavaTypeName(type));
	}

	public void DeclareLocalVar(String name, String type) throws ScriptException {
		scriptContext.addLocalVar(name, typeResolver.resolveJavaTypeName(type));
	}

	public void DeclareLocalVar(String name, String type, Object initValue) throws ScriptException {
		scriptContext.addLocalVar(name, typeResolver.resolveJavaTypeName(type), initValue);
	}

	public void SetLocalVar(String name, Object expr) throws ScriptException {
		scriptContext.updateLocalVar(name, getValue(expr));
	}

	public Object GetGlobalVar(String name) {
		return runtimeContext.getGlobalVar(name);
	}

	public void SetGlobalVar(String name, Object expr) throws ScriptException {
		runtimeContext.setGlobalVar(name, getValue(expr));
	}

	public boolean EqualCondition(Object expr1, Object expr2) throws ScriptException {
		return EqualCondition(expr1, expr2, null);
	}

	public boolean EqualCondition(Object expr1, Object expr2, String description) throws ScriptException {
		AbstractCondition condition = new EqualCondition(getValue(expr1), getValue(expr2));
		if (description != null) {
			condition.setDescription(description);
		}
		
		return condition.isTrue();
	}

	public boolean LessOrEqualCondition(Object expr1, Object expr2) throws ScriptException {
		return LessOrEqualCondition(expr1, expr2, null);
	}

	public boolean LessOrEqualCondition(Object expr1, Object expr2, String description) throws ScriptException {
		AbstractCondition condition = new LessOrEqualCondition(getValue(expr1), getValue(expr2));
		if (description != null) {
			condition.setDescription(description);
		}
		
		return condition.isTrue();
	}

	public ActionResult Action(String name, Object... params) throws ScriptException {
		Action action = typeResolver.resolveAction(name);
		action.setInputParameters(params);
		Object[] result = action.execute();
		ActionResult actionResult = new ActionResultImpl(result);
		
		return actionResult;
	}
	
	public void SubFlow(String name) throws ScriptException {
		Script subFlow = typeResolver.resolveSubflow(name);
		subFlow.execute();
	}

	public Expression<Boolean> AsBoolean(String input) {
		return new BooleanExpression(input);
	}

	public Expression<String> AsString(String input) {
		return new StringLiteralExpression(input);
	}

	public void setTypeResolver(TypeResolver typeResolver) {
		this.typeResolver = typeResolver;
	}
	
	public interface ActionResult {

		public ActionResult SetLocalVar(String name) throws ScriptException;

	}
	
	private Expression<?> toExpression(Object expr) throws ScriptException {
		if (expr instanceof Expression) {
			return (Expression<?>) expr;
		}
		else {
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
	
	private class ActionResultImpl implements ActionResult {
		
		private Object[] input;
		
		private int size;
		
		private int index;

		public ActionResultImpl(Object[] input) {
			this.input = input;
			size = input.length;
		}

		public ActionResult SetLocalVar(String name) throws ScriptException {
			if (index == size) {
				throw new ScriptException("No more elements found in action result");
			}
			Script.this.SetLocalVar(name, input[index++]);
			
			return this;
		}
		
	}

}
