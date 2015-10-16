package com.ilsid.bfa.compiler;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.ilsid.bfa.script.DynamicCodeInvocation;
import com.ilsid.bfa.script.Script;

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
 * Compiles classes in runtime.
 * 
 * @author illia.sydorovych
 *
 */
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
		ClassPool classPool = getClassPool();

		try {
			CtClass clazz = buildInvocationClass(className, expression, classPool);
			result = toClass(clazz);
		} catch (NotFoundException | CannotCompileException e) {
			throw new ClassCompilationException(String.format(
					"Compilation of Invocation class failed. Class [%s]. Expression [%s]", className, expression), e);
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
		ClassPool classPool = getClassPool();

		try {
			CtClass clazz = buildScriptClass(className, scriptBody, classPool);
			result = toClass(clazz);
		} catch (NotFoundException | CannotCompileException | IOException e) {

			throw new ClassCompilationException(String.format("Compilation of Script class [%s] failed", className), e);
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
		ByteArrayClassPath classPathEntry = new ByteArrayClassPath(className, byteCode);
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

	private static ClassPool getClassPool() {
		// TODO: non-default class pool may be needed
		return ClassPool.getDefault();
	}
	
	private static CtClass buildInvocationClass(String className, String expression, ClassPool classPool) throws NotFoundException, CannotCompileException {
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
	
	private static CtClass buildScriptClass(String className, InputStream scriptBody, ClassPool classPool)
			throws NotFoundException, CannotCompileException, IOException {
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

}
