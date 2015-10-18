package com.ilsid.bfa.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.ilsid.bfa.script.DynamicCodeInvocation;
import com.ilsid.bfa.script.Script;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Compiles classes in runtime.
 * 
 * @author illia.sydorovych
 *
 */
// TODO: unit tests for *toBytecode() methods
public class ClassCompiler {

	private static final String SCRIPT_CONTEXT_CLASS_NAME = "com.ilsid.bfa.script.ScriptContext";

	private static final String INVOCATION_INTERFACE_NAME = "com.ilsid.bfa.script.DynamicCodeInvocation";

	private static final String SCRIPT_CLASS_NAME = "com.ilsid.bfa.script.Script";

	private static final CtClass[] NO_ARGS = {};

	/**
	 * Compiles class that implements {@link DynamicCodeInvocation} interface.
	 * 
	 * @param className
	 *            class name
	 * @param expression
	 *            java source code (implementation)
	 * @return {@link DynamicCodeInvocation} implementation
	 * @throws ClassCompilationException
	 *             in case of compilation failure
	 */
	public static Class<?> compileInvocation(String className, String expression) throws ClassCompilationException {
		Class<?> result;

		try {
			CtClass clazz = buildInvocationClass(className, expression);
			result = toClass(clazz);
		} catch (NotFoundException | CannotCompileException e) {
			throw new ClassCompilationException(
					String.format("Compilation of Invocation class failed. Class [%s]. ValueExpression [%s]", className,
							expression),
					e);
		}

		return result;
	}

	/**
	 * Compiles a byte code for a class that implements
	 * {@link DynamicCodeInvocation} interface.
	 * 
	 * @param className
	 *            class name
	 * @param expression
	 *            java source code (implementation)
	 * @return a byte code for {@link DynamicCodeInvocation} implementation
	 * @throws ClassCompilationException
	 *             in case of compilation failure
	 */
	public static byte[] compileInvocationToBytecode(String className, String expression)
			throws ClassCompilationException {
		byte[] result;

		try {
			CtClass clazz = buildInvocationClass(className, expression);
			result = toBytecode(clazz);
		} catch (NotFoundException | CannotCompileException | IOException e) {
			throw new ClassCompilationException(
					String.format("Compilation of Invocation to byte code failed. Class [%s]. ValueExpression [%s]",
							className, expression),
					e);
		}

		return result;
	}

	/**
	 * Compiles class that extends {@link Script} class.
	 * 
	 * @param className
	 *            class name
	 * @param scriptBody
	 *            java source code (implementation)
	 * @return {@link Script} descendant
	 * @throws ClassCompilationException
	 *             in case of compilation failure
	 */
	public static Class<?> compileScript(String className, InputStream scriptBody) throws ClassCompilationException {
		Class<?> result;

		try {
			CtClass clazz = buildScriptClass(className, scriptBody);
			result = toClass(clazz);
		} catch (NotFoundException | CannotCompileException | IOException e) {

			throw new ClassCompilationException(String.format("Compilation of Script class [%s] failed", className), e);
		}

		return result;
	}

	/**
	 * Compiles a byte code for a class that extends {@link Script} class.
	 * 
	 * @param className
	 *            class name
	 * @param scriptBody
	 *            java source code (implementation)
	 * @return a byte code for {@link Script} descendant
	 * @throws ClassCompilationException
	 *             in case of compilation failure
	 */
	public static byte[] compileScriptToBytecode(String className, InputStream scriptBody)
			throws ClassCompilationException {
		byte[] result;

		try {
			CtClass clazz = buildScriptClass(className, scriptBody);
			result = toBytecode(clazz);
		} catch (NotFoundException | CannotCompileException | IOException e) {

			throw new ClassCompilationException(
					String.format("Compilation of Script [%s] to byte code failed", className), e);
		}

		return result;
	}

	/**
	 * Creates {@link Class} object from the given byte code.
	 * 
	 * @param className
	 *            class name
	 * @param byteCode
	 *            byte code
	 * @return {@link Class} object
	 * @throws ClassCompilationException
	 *             in case of compilation failure
	 */
	public static Class<?> loadFromBytecode(String className, byte[] byteCode) throws ClassCompilationException {
		ClassPath classPathEntry = new ByteArrayClassPath(className, byteCode);
		ClassPool classPool = getClassPool();
		classPool.appendClassPath(classPathEntry);
		Class<?> result;
		try {
			CtClass clazz = classPool.get(className);
			result = toClass(clazz);
			classPool.removeClassPath(classPathEntry);
		} catch (NotFoundException | CannotCompileException e) {
			throw new ClassCompilationException(String.format("Failed to create class %s from byte code", className),
					e);
		}

		return result;
	}

	/**
	 * Compiles all dynamic expressions defined in a given script.
	 * 
	 * @param scriptClassName
	 *            script class name
	 * @param scriptByteCode
	 *            script byte code
	 * @return a list of {@link CompilationUnit} instances corresponding to each
	 *         expression or an empty list, if no expressions are defined
	 * @throws ClassCompilationException
	 *             if compilation of any expression failed
	 */
	//TODO: complete implementation
	public static List<CompilationUnit> compileScriptExpressions(String scriptClassName, byte[] scriptByteCode)
			throws ClassCompilationException {
		ClassPath classPathEntry = new ByteArrayClassPath(scriptClassName, scriptByteCode);
		ClassPool classPool = getClassPool();
		classPool.appendClassPath(classPathEntry);
		CtClass clazz;
		try {
			clazz = classPool.get(scriptClassName);
		} catch (NotFoundException e) {
			throw new ClassCompilationException(String.format("Script class [%s] not found", scriptClassName), e);
		}

		if (clazz.isFrozen()) {
			clazz.defrost();
		}

		CtMethod method;
		try {
			method = clazz.getMethod("doExecute", Descriptor.ofMethod(CtClass.voidType, NO_ARGS));
		} catch (NotFoundException e) {
			throw new ClassCompilationException(
					String.format("Method [%s] not found in class [%s]", "doExecute()", scriptClassName), e);
		}

		try {
			method.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					System.out.println(m.getMethodName());
				}
			});
		} catch (CannotCompileException e) {
			throw new ClassCompilationException(
					String.format("Failed to compile expressions in the script [%s]", scriptClassName), e);
		}

		classPool.removeClassPath(classPathEntry);

		return null;
	}

	private static ClassPool getClassPool() {
		// TODO: non-default class pool may be needed
		return ClassPool.getDefault();
	}

	private static CtClass buildInvocationClass(String className, String expression)
			throws NotFoundException, CannotCompileException {
		ClassPool classPool = getClassPool();
		CtClass clazz = classPool.makeClass(className);
		clazz.addInterface(classPool.get(INVOCATION_INTERFACE_NAME));

		CtClass scriptContextClass = classPool.get(SCRIPT_CONTEXT_CLASS_NAME);
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
		method.setBody(expression);
		clazz.addMethod(method);

		return clazz;
	}

	private static CtClass buildScriptClass(String className, InputStream scriptBody)
			throws NotFoundException, CannotCompileException, IOException {
		ClassPool classPool = getClassPool();
		CtClass clazz = classPool.makeClass(className);
		clazz.setSuperclass(classPool.get(SCRIPT_CLASS_NAME));

		CtConstructor cons = new CtConstructor(NO_ARGS, clazz);
		cons.setBody(";");
		clazz.addConstructor(cons);

		CtMethod method = new CtMethod(CtClass.voidType, "doExecute", NO_ARGS, clazz);
		method.setModifiers(Modifier.PROTECTED);
		String body = "{" + StringUtils.LF + IOUtils.toString(scriptBody, "UTF-8") + StringUtils.LF + "}";
		method.setBody(body);
		clazz.addMethod(method);

		return clazz;
	}

	private static Class<?> toClass(CtClass clazz) throws CannotCompileException {
		// TODO: Here, a custom class loader may be needed if deploying on
		// application server
		Class<?> result = clazz.toClass();
		clazz.detach();
		return result;
	}

	private static byte[] toBytecode(CtClass clazz) throws CannotCompileException, IOException {
		byte[] result = clazz.toBytecode();
		clazz.detach();
		return result;
	}

}
