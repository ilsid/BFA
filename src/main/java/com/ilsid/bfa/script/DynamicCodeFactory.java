package com.ilsid.bfa.script;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

/**
 * Utility for dynamic Java code processing.
 * 
 * @author illia.sydorovych
 *
 */
class DynamicCodeFactory {

	static String GENERATED_PACKAGE = "com.ilsid.bfa.generated.";

	private static final CtClass[] NO_ARGS = {};

	private static Map<String, String> replaceableSymbols = new HashMap<>();

	private static Map<String, Class<DynamicCodeInvocation>> invocationCache = new HashMap<>();

	private static Map<String, Class<Script>> scriptCache = new HashMap<>();

	static {
		replaceableSymbols.put("-", "_Mns_");
		replaceableSymbols.put("+", "_Pls_");
		replaceableSymbols.put("*", "_Mlt_");
		replaceableSymbols.put("/", "_Div_");
		replaceableSymbols.put(".", "%dt");
	}

	/**
	 * Returns {@link DynamicCodeInvocation} instance for given java expression.
	 * 
	 * @param scriptName
	 *            name of script where this expression is defined
	 * @param scriptExpression
	 *            original scripting expression
	 * @param javaExpression
	 *            java expression
	 * @return {@link DynamicCodeInvocation} instance that executes the java
	 *         expression
	 * @throws DynamicCodeException
	 *             in case of compilation or instantiation failure
	 */
	public static DynamicCodeInvocation getInvocation(String scriptName, String scriptExpression, String javaExpression)
			throws DynamicCodeException {

		String className = generateClassName(scriptName, scriptExpression);
		DynamicCodeInvocation invocation = tryInstantiateFromCache(className, invocationCache);

		if (invocation != null) {
			return invocation;
		}

		ClassPool pool = ClassPool.getDefault();
		CtClass clazz = pool.makeClass(className);
		try {
			clazz.addInterface(pool.get(DynamicCodeInvocation.class.getName()));

			CtClass scriptContextClass = pool.get(ScriptContext.class.getName());
			CtField field = new CtField(scriptContextClass, "scriptContext", clazz);
			field.setModifiers(Modifier.PRIVATE);
			clazz.addField(field);

			CtConstructor cons = new CtConstructor(NO_ARGS, clazz);
			cons.setBody(";");
			clazz.addConstructor(cons);

			CtMethod method;
			method = new CtMethod(CtClass.voidType, "setScriptContext", new CtClass[] { scriptContextClass }, clazz);
			method.setBody("scriptContext = $1;");
			clazz.addMethod(method);

			method = new CtMethod(pool.get(Object.class.getName()), "invoke", NO_ARGS, clazz);
			method.setBody(javaExpression);
			clazz.addMethod(method);

			byte[] bytes = clazz.toBytecode();
			pool.appendClassPath(new ByteArrayClassPath(className, bytes));
			clazz = pool.get(className);

			@SuppressWarnings("unchecked")
			Class<DynamicCodeInvocation> invocationClass = clazz.toClass();
			putToCache(className, invocationClass, invocationCache);
			invocation = (DynamicCodeInvocation) invocationClass.newInstance();

		} catch (InstantiationException | IllegalAccessException | NotFoundException | CannotCompileException
				| IOException e) {

			throw new DynamicCodeException(
					"Failed to create class for expression [" + scriptExpression + "] in script [" + scriptName + "]",
					e);
		}

		return invocation;
	}

	/**
	 * Returns {@link Script} instance with given java source code.
	 * 
	 * @param scriptName
	 *            script name
	 * @param scriptBody
	 *            java source code
	 * @return {@link Script} instance that executes the given code
	 * @throws DynamicCodeException
	 *             in case of compilation or instantiation failure
	 */
	public static Script getScript(String scriptName, InputStream scriptBody) throws DynamicCodeException {
		String className = generateClassName(scriptName);
		Script script = tryInstantiateFromCache(className, scriptCache);

		if (script != null) {
			return script;
		}

		ClassPool pool = ClassPool.getDefault();
		CtClass clazz = pool.makeClass(className);
		try {
			clazz.setSuperclass(pool.get(Script.class.getName()));

			CtConstructor cons = new CtConstructor(NO_ARGS, clazz);
			cons.setBody(";");
			clazz.addConstructor(cons);

			CtMethod method = new CtMethod(CtClass.voidType, "doExecute", NO_ARGS, clazz);
			method.setModifiers(Modifier.PROTECTED);
			String body = "{" + IOUtils.toString(scriptBody, "UTF-8") + "}";
			method.setBody(body);
			clazz.addMethod(method);

			byte[] bytes = clazz.toBytecode();
			pool.appendClassPath(new ByteArrayClassPath(className, bytes));
			clazz = pool.get(className);

			@SuppressWarnings("unchecked")
			Class<Script> scriptClass = clazz.toClass();
			putToCache(className, scriptClass, scriptCache);
			script = (Script) scriptClass.newInstance();

		} catch (NotFoundException | CannotCompileException | InstantiationException | IllegalAccessException
				| IOException e) {

			throw new DynamicCodeException("Failed to create class for script [" + scriptName + "]", e);
		}

		return script;
	}

	static String generateClassName(String scriptName, String expression) {
		return GENERATED_PACKAGE + generateSimpleClassName(scriptName) + "$" + generateSimpleClassName(expression);
	}

	static String generateClassName(String scriptName) {
		return GENERATED_PACKAGE + generateSimpleClassName(scriptName);
	}

	private static String generateSimpleClassName(String expression) {
		String expr = expression.replaceAll("\\s", "");
		for (String smb : replaceableSymbols.keySet()) {
			expr = expr.replace(smb, replaceableSymbols.get(smb));
		}

		return expr;
	}

	private static synchronized <T> T tryInstantiateFromCache(String className, Map<String, Class<T>> cache)
			throws DynamicCodeException {

		Class<T> clazz = cache.get(className);
		T instance = tryCreateInstance(clazz);
		return instance;
	}

	private static <T> T tryCreateInstance(Class<T> clazz) throws DynamicCodeException {
		T instance = null;
		if (clazz != null) {
			try {
				instance = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new DynamicCodeException("Failed to create instance of " + clazz.getName(), e);
			}
		}

		return instance;
	}

	private static synchronized <T> void putToCache(String className, Class<T> clazz, Map<String, Class<T>> cache) {
		if (cache.get(className) == null) {
			cache.put(className, clazz);
		}
	}

}
