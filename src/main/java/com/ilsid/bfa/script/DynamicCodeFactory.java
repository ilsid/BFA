package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.runtime.GlobalContext;

/**
 * Utility for dynamic Java code processing.
 * 
 * @author illia.sydorovych
 *
 */
public class DynamicCodeFactory {

	private static Map<String, Class<?>> cache = new HashMap<>();

	/**
	 * Returns {@link DynamicCodeInvocation} instance for the given script
	 * expression. If a code repository is defined in the global
	 * {@link GlobalContext}, then the corresponding class is searched by the
	 * "script name/expression" combination in the repository. Otherwise, the
	 * expression is parsed and translated into Java code by the specified
	 * parser and the expression class is built on-the-fly.
	 * 
	 * @param scriptName
	 *            script name
	 * @param scriptExpression
	 *            script expression
	 * @param parser
	 *            script expression parser
	 * @return {@link DynamicCodeInvocation} instance that executes the given
	 *         expression
	 * @throws DynamicCodeException
	 *             <ul>
	 *             <li>if the expression parsing is failed</li>
	 *             <li>if the on-the-fly compilation is failed</li>
	 *             <li>if a code repository is defined, but no needed class was
	 *             found there</li>
	 *             <li>if the class instantiation failed</li>
	 *             </ul>
	 */
	public static DynamicCodeInvocation getInvocation(String scriptName, String scriptExpression,
			ScriptExpressionParser parser) throws DynamicCodeException {

		String className = ClassNameUtil.resolveExpressionClassName(scriptName, scriptExpression);

		DynamicCodeInvocation invocation = (DynamicCodeInvocation) tryInstantiateFromCache(className);
		if (invocation != null) {
			return invocation;
		}

		try {
			invocation = tryInstantiateFromRepository(className, DynamicCodeInvocation.class);
		} catch (LoadFromRepositoryException e) {
			throw new DynamicCodeException("Failed to load the expression [" + scriptExpression + "] in the script ["
					+ scriptName + "] from the repository", e.getCause());
		}

		if (invocation != null) {
			return invocation;
		}

		String javaExpression = parser.parse(scriptExpression);

		try {
			invocation = (DynamicCodeInvocation) createInstance(
					new InvocationCompilerDelegate(className, javaExpression));
		} catch (InstantiationException | IllegalAccessException | ClassCompilationException e) {
			throw new DynamicCodeException("Failed to create instance of the expression [" + scriptExpression
					+ "] in the script [" + scriptName + "]", e);
		}

		return invocation;
	}

	/**
	 * Returns {@link Script} instance that executes the given Java code. If a
	 * code repository is defined in the global {@link GlobalContext}, then the
	 * corresponding class is searched by the script name in this repository.
	 * Otherwise, the class is built on-the-fly.
	 * 
	 * @param scriptName
	 *            script name
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
	public static Script getScript(String scriptName, String scriptBody) throws DynamicCodeException {
		String className = ClassNameUtil.resolveScriptClassName(scriptName);

		Script script = (Script) tryInstantiateFromCache(className);
		if (script != null) {
			return script;
		}

		try {
			script = tryInstantiateFromRepository(className, Script.class);
		} catch (LoadFromRepositoryException e) {
			throw new DynamicCodeException("Failed to load the script [" + scriptName + "] from the repository",
					e.getCause());
		}

		if (script != null) {
			return script;
		}

		try {
			script = (Script) createInstance(new ScriptCompilerDelegate(className, scriptBody));
		} catch (InstantiationException | IllegalAccessException | ClassCompilationException e) {
			throw new DynamicCodeException("Failed to create instance of the script [" + scriptName + "]", e);
		}

		return script;
	}

	@SuppressWarnings("unchecked")
	private static <T> T tryInstantiateFromRepository(String className, Class<T> instanceClass)
			throws DynamicCodeException, LoadFromRepositoryException {

		T instance = null;
		CodeRepository codeRepository = GlobalContext.getInstance().getCodeRepository();

		if (codeRepository != null) {
			try {
				instance = (T) instantiateFromRepository(className, codeRepository);
			} catch (InstantiationException | IllegalAccessException | ClassCompilationException e) {
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

	private static Object instantiateFromRepository(String className, CodeRepository codeRepository)
			throws DynamicCodeException, InstantiationException, IllegalAccessException, ClassCompilationException {

		byte[] byteCode;
		try {
			byteCode = codeRepository.load(className);
		} catch (PersistenceException e) {
			throw new DynamicCodeException("Failed to load class [" + className + "] from repository", e);
		}

		if (byteCode.length == 0) {
			throw new DynamicCodeException("Class [" + className + "] does not exist in repository");
		}
		
		Object instance = createInstance(new LoadFromBytecodeDelegate(className, byteCode));
		return instance;
	}

	private static Object createInstance(ClassCompilerDelegate compiler)
			throws ClassCompilationException, InstantiationException, IllegalAccessException {
		String className = compiler.getClassName();
		Class<?> clazz;

		synchronized (className.intern()) {
			Class<?> alreadyInCacheClass = cache.get(className);
			if (alreadyInCacheClass == null) {
				clazz = compiler.compile();
				cache.put(className, clazz);
			} else {
				clazz = alreadyInCacheClass;
			}
		}

		Object instance = clazz.newInstance();
		return instance;
	}

	@SuppressWarnings("serial")
	private static class LoadFromRepositoryException extends Exception {

		public LoadFromRepositoryException(Throwable cause) {
			super(cause);
		}
	}

	private static interface ClassCompilerDelegate {

		Class<?> compile() throws ClassCompilationException;

		String getClassName();
	}

	private static class InvocationCompilerDelegate implements ClassCompilerDelegate {

		private String className;

		private String javaExpression;

		public InvocationCompilerDelegate(String className, String javaExpression) {
			this.className = className;
			this.javaExpression = javaExpression;
		}

		public Class<?> compile() throws ClassCompilationException {
			return ClassCompiler.compileInvocation(className, javaExpression);
		}

		public String getClassName() {
			return className;
		}

	}

	private static class ScriptCompilerDelegate implements ClassCompilerDelegate {

		private String className;

		private String scriptBody;

		public ScriptCompilerDelegate(String className, String scriptBody) {
			this.className = className;
			this.scriptBody = scriptBody;
		}

		public Class<?> compile() throws ClassCompilationException {
			return ClassCompiler.compileScript(className, scriptBody);
		}

		public String getClassName() {
			return className;
		}

	}
	
	private static class LoadFromBytecodeDelegate implements ClassCompilerDelegate {

		private String className;

		private byte[] byteCode;

		public LoadFromBytecodeDelegate(String className, byte[] byteCode) {
			this.className = className;
			this.byteCode = byteCode;
		}

		public Class<?> compile() throws ClassCompilationException {
			return ClassCompiler.loadFromBytecode(className, byteCode);
		}

		public String getClassName() {
			return className;
		}

	}

}
