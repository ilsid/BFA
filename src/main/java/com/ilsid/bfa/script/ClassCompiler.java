package com.ilsid.bfa.script;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.ExceptionUtil;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.script.ScriptSourcePreprocessor.ExpressionsUnit;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;

/**
 * Compiles scripting classes.
 * 
 * @author illia.sydorovych
 *
 */
public class ClassCompiler {

	private static final String EXPRESSION_ERROR_MESSAGE_TEMPLATE = "Failed to parse expression in script [{}]";

	private static final String DEFAULT_CONSTRUCTOR_TEMPLATE = "new %s();";

	private static final CtClass[] NO_ARGS = {};

	private static final ClassPool classPool;

	private static Logger logger;

	static {
		classPool = ClassPool.getDefault();
		classPool.appendClassPath(new ClassClassPath(CompilerConstants.SCRIPT_CLASS));
	}

	/**
	 * Compiles {@link Script} descendant.
	 * 
	 * @param className
	 *            class name
	 * @param scriptBody
	 *            java source code (the contents of {@link Script#doExecute()} method)
	 * @return a byte code for {@link Script} descendant
	 * @throws ClassCompilationException
	 *             in case of compilation failure
	 */
	// FIXME: No compile-time check of value type for SetLocalVar() call. For example, this is legal in compile-time,
	// but causes expected error in runtime:

	// DeclareLocalVar("Var5", "Number");
	// SetLocalVar("Var5", "1.77");
	public static synchronized ScriptCompilationUnit compileScript(String className, String scriptBody)
			throws ClassCompilationException {

		ScriptCompilationUnit result = new ScriptCompilationUnit();
		byte[] byteCode;

		try {
			CtClass clazz = buildScriptClass(className, scriptBody, result);
			byteCode = toBytecode(clazz);
		} catch (NotFoundException | CannotCompileException | IOException | ParsingException e) {

			throw new ClassCompilationException(
					String.format("Compilation of script [%s] failed", ClassNameUtil.getShortClassName(className)), e);
		}

		result.byteCode = byteCode;

		return result;
	}

	/**
	 * Compiles entity using the specified class name and body. The source code to compile will have the following
	 * format: </br>
	 * </br>
	 * <code>
	 * &lt;className&gt; {
	 * </br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;
	 * &lt;fields defined in entityBody&gt;
	 * </br>
	 * }  
	 * </code> </br>
	 * </br>
	 * The following entity body format is expected:</br>
	 * </br>
	 * <code>type name;[type name;[type name[...]]]</code> </br>
	 * </br>
	 * The entity body example: </br>
	 * </br>
	 * <code>java.lang.Integer Days;java.lang.Integer ProlongDays;java.lang.Double MonthlyFee;</code>
	 * 
	 * @param className
	 *            class name
	 * @param entityBody
	 *            java source code.
	 * 
	 * @return the entity byte code
	 * @throws ClassCompilationException
	 *             in case of compilation failure
	 */
	public static synchronized byte[] compileEntity(String className, String entityBody)
			throws ClassCompilationException {
		byte[] result;
		ClassPool classPool = getClassPool();
		CtClass clazz = classPool.makeClass(className);

		// Dynamic class loader is needed in case when the entity's field types are generated itself. In this case,
		// this loader must be used to resolve dependencies
		final LoaderClassPath dynamicClassPath = new LoaderClassPath(DynamicClassLoader.getInstance());
		classPool.appendClassPath(dynamicClassPath);

		try {
			String[] fieldExpressions = entityBody.split(";");
			for (String fieldExpr : fieldExpressions) {
				String trimmedExpr = fieldExpr.trim();
				String[] exprParts = trimmedExpr.split("\\s+");
				if (exprParts.length != 2) {
					throw new ClassCompilationException(String.format(
							"Compilation of Entity [%s] failed. Expression [%s] is invalid", className, trimmedExpr));
				}
				String fieldType = exprParts[0];
				String fieldName = exprParts[1];

				CtClass fieldTypeClass = classPool.get(fieldType);
				CtField field = new CtField(fieldTypeClass, fieldName, clazz);
				field.setModifiers(Modifier.PUBLIC);

				if (hasDefaultPublicConstructorAndIsNotPredefined(fieldType)) {
					clazz.addField(field, String.format(DEFAULT_CONSTRUCTOR_TEMPLATE, fieldType));
				} else {
					clazz.addField(field);
				}
			}

			result = toBytecode(clazz);

		} catch (NotFoundException | CannotCompileException | IOException | IllegalStateException
				| ClassNotFoundException e) {

			throw new ClassCompilationException(String.format("Compilation of Entity [%s] failed", className), e);

		} finally {
			classPool.removeClassPath(dynamicClassPath);
		}

		return result;
	}

	/**
	 * Defines the logger implementation.
	 * 
	 * @param loggerImpl
	 *            the logger instance
	 */
	@Inject
	public static void setLogger(@ScriptLogger Logger loggerImpl) {
		logger = loggerImpl;
	}

	/**
	 * A script compilation unit.
	 *
	 */
	public static class ScriptCompilationUnit {

		private byte[] byteCode;

		private Map<String, String> inputParameters;

		private String generatedSource;

		public byte[] getByteCode() {
			return byteCode;
		}

		public Map<String, String> getInputParameters() {
			return inputParameters;
		}

		public String getGeneratedSource() {
			return generatedSource;
		}

	}

	private static ClassPool getClassPool() {
		return classPool;
	}

	private static CtClass buildScriptClass(String className, String scriptBody,
			final ScriptCompilationUnit compilationUnit)
			throws NotFoundException, CannotCompileException, IOException, ClassCompilationException, ParsingException {

		processExpressions(ScriptSourcePreprocessor.processVarargs(scriptBody), className, compilationUnit);

		ClassPool classPool = getClassPool();
		CtClass clazz = classPool.makeClass(className);
		clazz.setSuperclass(classPool.get(CompilerConstants.SCRIPT_CLASS_NAME));

		CtConstructor cons = new CtConstructor(NO_ARGS, clazz);
		cons.setBody(";");
		clazz.addConstructor(cons);

		CtMethod method = new CtMethod(CtClass.voidType, CompilerConstants.SCRIPT_IMPLEMENTATION_METHOD_NAME, NO_ARGS,
				clazz);
		method.setModifiers(Modifier.PROTECTED);

		// Dynamic class loader is needed in case when some script expressions refer to generated types (entities)
		final LoaderClassPath dynamicClassPath = new LoaderClassPath(DynamicClassLoader.getInstance());
		classPool.appendClassPath(dynamicClassPath);
		try {
			method.setBody(compilationUnit.generatedSource);
			clazz.addMethod(method);
		} finally {
			classPool.removeClassPath(dynamicClassPath);
		}

		return clazz;
	}

	private static void processExpressions(String source, String className, ScriptCompilationUnit compilationUnit)
			throws ClassCompilationException, ParsingException {

		ExpressionsUnit expressionsUnit = ScriptSourcePreprocessor.processExpressions(source);
		String shortClassName = ClassNameUtil.getShortClassName(className);

		final List<Exception> processingErrors = expressionsUnit.getProcessingErrors();
		if (!processingErrors.isEmpty()) {
			if (logger != null) {
				for (Exception e : processingErrors) {
					logger.error(EXPRESSION_ERROR_MESSAGE_TEMPLATE, shortClassName, e);
				}
			}

			throw new ClassCompilationException(
					String.format("Script [%s] contains errors. Compilation failed", shortClassName) + StringUtils.LF
							+ mergeErrorMessages(processingErrors));
		}

		compilationUnit.generatedSource = expressionsUnit.getSource();
		compilationUnit.inputParameters = expressionsUnit.getInputParameters();
	}

	private static byte[] toBytecode(CtClass clazz) throws CannotCompileException, IOException {
		byte[] result = clazz.toBytecode();
		clazz.detach();
		return result;
	}

	private static String mergeErrorMessages(List<Exception> exceptions) {
		StringBuilder messages = new StringBuilder();
		for (Exception e : exceptions) {
			messages.append(ExceptionUtil.getExceptionMessageChain(e)).append(StringUtils.LF);
		}

		return messages.toString();
	}

	private static boolean hasDefaultPublicConstructorAndIsNotPredefined(String className)
			throws ClassNotFoundException, IllegalStateException {

		if (PredefinedTypes.isPredefinedJavaType(className)) {
			return false;
		}

		Class<?> clazz = DynamicClassLoader.getInstance().loadClass(className);

		for (Constructor<?> constructor : clazz.getConstructors()) {
			if (constructor.getParameterTypes().length == 0) {
				return true;
			}
		}

		return false;
	}

}
