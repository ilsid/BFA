package com.ilsid.bfa.script;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.PersistenceException;

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
// TODO: change caching mechanism
class DynamicCodeFactory {

	static final String GENERATED_PACKAGE = "com.ilsid.bfa.generated.";

	private static final CtClass[] NO_ARGS = {};

	private static Map<String, String> replaceableSymbols = new HashMap<>();

	private static Map<String, Class<?>> cache = new HashMap<>();

	static {
		replaceableSymbols.put("-", "_Mns_");
		replaceableSymbols.put("+", "_Pls_");
		replaceableSymbols.put("*", "_Mlt_");
		replaceableSymbols.put("/", "_Div_");
		replaceableSymbols.put(".", "%dt");
	}

	/**
	 * Returns {@link DynamicCodeInvocation} instance for given java expression.
	 * If a code repository is defined in the passed script context, then the
	 * corresponding class is searched in this repository. Otherwise, the class
	 * is built on-the-fly.
	 * 
	 * @param scriptContext
	 *            context of this scripting expression
	 * @param scriptExpression
	 *            original scripting expression
	 * @param javaExpression
	 *            java expression
	 * @return {@link DynamicCodeInvocation} instance that executes the java
	 *         expression
	 * @throws DynamicCodeException
	 *             <ul>
	 *             <li>if the on-the-fly compilation failed</li>
	 *             <li>if a code repository is defined, but no needed class was
	 *             found there</li>
	 *             <li>if the class instantiation failed</li>
	 *             </ul>
	 */
	public static DynamicCodeInvocation getInvocation(ScriptContext scriptContext, String scriptExpression,
			String javaExpression) throws DynamicCodeException {

		String scriptName = scriptContext.getScriptName();
		String className = generateClassName(scriptName, scriptExpression);

		DynamicCodeInvocation invocation = (DynamicCodeInvocation) tryInstantiateFromCache(className);
		if (invocation != null) {
			return invocation;
		}

		ClassPool classPool = ClassPool.getDefault();
		try {
			invocation = tryInstantiateFromRepository(className, scriptContext, classPool, DynamicCodeInvocation.class);
		} catch (LoadFromRepositoryException e) {
			throw new DynamicCodeException("Failed to load the expression [" + scriptExpression + "] in the script ["
					+ scriptName + "] from the repository", e.getCause());
		}

		if (invocation != null) {
			return invocation;
		}

		try {
			CtClass clazz = classPool.makeClass(className);
			clazz.addInterface(classPool.get(DynamicCodeInvocation.class.getName()));

			CtClass scriptContextClass = classPool.get(ScriptContext.class.getName());
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

			method = new CtMethod(classPool.get(Object.class.getName()), "invoke", NO_ARGS, clazz);
			method.setBody(javaExpression);
			clazz.addMethod(method);

			invocation = (DynamicCodeInvocation) createInstance(clazz);

		} catch (InstantiationException | IllegalAccessException | NotFoundException | CannotCompileException
				| IOException e) {

			throw new DynamicCodeException("Failed to create instance of the expression [" + scriptExpression
					+ "] in the script [" + scriptName + "]", e);
		}

		return invocation;
	}

	/**
	 * Returns {@link Script} instance with given java source code.
	 * If a code repository is defined in the passed script context, then the
	 * corresponding class is searched in this repository. Otherwise, the class
	 * is built on-the-fly.
	 * 
	 * @param scriptContext
	 *            context of this scripting expression
	 * @param scriptBody
	 *            java source code
	 * @return {@link Script} instance that executes the given code
	 * @throws DynamicCodeException
	 *             <ul>
	 *             <li>if the on-the-fly compilation failed</li>
	 *             <li>if a code repository is defined, but no needed class was
	 *             found there</li>
	 *             <li>if the class instantiation failed</li>
	 *             </ul>
	 */
	public static Script getScript(ScriptContext scriptContext, InputStream scriptBody) throws DynamicCodeException {
		String scriptName = scriptContext.getScriptName();
		String className = generateClassName(scriptName);

		Script script = (Script) tryInstantiateFromCache(className);
		if (script != null) {
			return script;
		}

		ClassPool classPool = ClassPool.getDefault();
		try {
			script = tryInstantiateFromRepository(className, scriptContext, classPool, Script.class);
		} catch (LoadFromRepositoryException e) {
			throw new DynamicCodeException("Failed to load the script [" + scriptName + "] from the repository",
					e.getCause());
		}
		
		if (script != null) {
			return script;
		}

		try {
			CtClass clazz = classPool.makeClass(className);
			clazz.setSuperclass(classPool.get(Script.class.getName()));

			CtConstructor cons = new CtConstructor(NO_ARGS, clazz);
			cons.setBody(";");
			clazz.addConstructor(cons);

			CtMethod method = new CtMethod(CtClass.voidType, "doExecute", NO_ARGS, clazz);
			method.setModifiers(Modifier.PROTECTED);
			String body = "{" + IOUtils.toString(scriptBody, "UTF-8") + "}";
			method.setBody(body);
			clazz.addMethod(method);

			script = (Script) createInstance(clazz);

		} catch (NotFoundException | CannotCompileException | InstantiationException | IllegalAccessException
				| IOException e) {

			throw new DynamicCodeException("Failed to create instance of the script [" + scriptName + "]", e);
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

	@SuppressWarnings("unchecked")
	private static <T> T tryInstantiateFromRepository(String className, ScriptContext scriptContext,
			ClassPool classPool, Class<T> instanceClass) throws DynamicCodeException, LoadFromRepositoryException {

		T instance = null;
		CodeRepository codeRepository = scriptContext.getGlobalContext().getCodeRepository();

		if (codeRepository != null) {
			try {
				instance = (T) instantiateFromRepository(className, codeRepository, classPool);
			} catch (InstantiationException | IllegalAccessException | NotFoundException | CannotCompileException e) {
				throw new LoadFromRepositoryException(e);
			} catch (ClassCastException e) {
				throw new DynamicCodeException(
						"Unexpected class was loaded from the repository. Expected: " + instanceClass.getName(), e);
			}
		}

		return instance;
	}

	private static Object tryInstantiateFromCache(String className) throws DynamicCodeException {
		Class<?> clazz = cache.get(className);
		Object instance = null;

		if (clazz != null) {
			try {
				instance = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new DynamicCodeException("Failed to create instance of " + clazz.getName(), e);
			}
		}

		return instance;
	}

	private static Object instantiateFromRepository(String className, CodeRepository codeRepository,
			ClassPool classPool) throws DynamicCodeException, NotFoundException, CannotCompileException,
					InstantiationException, IllegalAccessException {

		byte[] byteCode;
		try {
			byteCode = codeRepository.load(className);
		} catch (PersistenceException e) {
			throw new DynamicCodeException("Failed to load class [" + className + "] from repository", e);
		}

		if (byteCode.length == 0) {
			throw new DynamicCodeException("Class [" + className + "] does not exist in repository");
		}

		// FIXME: not thread safe
		classPool.appendClassPath(new ByteArrayClassPath(className, byteCode));
		CtClass clazz = classPool.get(className);
		Class<?> instanceClass = clazz.toClass();
		cache.put(className, instanceClass);
		Object instance = instanceClass.newInstance();

		return instance;
	}

	private static Object createInstance(CtClass ctClass)
			throws CannotCompileException, InstantiationException, IllegalAccessException, IOException {
		Object instance;
		Class<?> instanceClass;

		// TODO: Think of ConcurrentHashMap usage
		synchronized (cache) {
			String className = ctClass.getName();
			Class<?> alreadyInCacheClass = cache.get(className);
			if (alreadyInCacheClass == null) {
				instanceClass = ctClass.toClass();
				cache.put(className, instanceClass);
			} else {
				instanceClass = alreadyInCacheClass;
			}
		}

		instance = instanceClass.newInstance();
		return instance;
	}

	@SuppressWarnings("serial")
	private static class LoadFromRepositoryException extends Exception {

		public LoadFromRepositoryException(Throwable cause) {
			super(cause);
		}
	}

}
