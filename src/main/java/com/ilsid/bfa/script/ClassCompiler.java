package com.ilsid.bfa.script;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.ExceptionUtil;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPath;
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
// FIXME: resolve cyclic package dependencies
public class ClassCompiler {

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
	 * Compiles all expressions defined in the given script.
	 * 
	 * @param scriptShortClassName
	 *            the script short class name (without package prefix)
	 * @param scriptSourceCode
	 *            the source code of the script class
	 * @return a collection of {@link CompilationBlock} instances for each
	 *         expression
	 * @throws ClassCompilationException
	 *             if the compilation of any expression failed
	 */
	public static Collection<CompilationBlock> compileScriptExpressions(String scriptShortClassName,
			InputStream scriptSourceCode) throws ClassCompilationException {
		CompilationUnit compilationUnit;
		try {
			compilationUnit = JavaParser.parse(scriptSourceCode);
		} catch (ParseException e) {
			throw new ClassCompilationException(
					String.format("Failed to parse a source code of the class [%s]", scriptShortClassName), e);
		}

		MethodCallVisitorContext visitorContext = new MethodCallVisitorContext();
		visitorContext.scriptShortClassName = scriptShortClassName;

		new MethodCallVisitor().visit(compilationUnit, visitorContext);

		List<Exception> exceptions = visitorContext.exceptions;
		if (!exceptions.isEmpty()) {
			throw new ClassCompilationException(
					String.format("Compilation of expressions in script [%s] failed", scriptShortClassName)
							+ StringUtils.LF + mergeMessages(exceptions));
		}

		return visitorContext.compiledExpressions.values();
	}

	private static ClassPool getClassPool() {
		// TODO: non-default class pool may be needed
		return ClassPool.getDefault();
	}

	private static CtClass buildInvocationClass(String className, String expression)
			throws NotFoundException, CannotCompileException {
		ClassPool classPool = getClassPool();
		CtClass clazz = classPool.makeClass(className);
		clazz.addInterface(classPool.get(CompilerConstants.INVOCATION_INTERFACE_NAME));

		CtClass scriptContextClass = classPool.get(CompilerConstants.SCRIPT_CONTEXT_CLASS_NAME);
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
		clazz.setSuperclass(classPool.get(CompilerConstants.SCRIPT_CLASS_NAME));

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

	private static String mergeMessages(List<Exception> exceptions) {
		StringBuilder messages = new StringBuilder();
		for (Exception e : exceptions) {
			messages.append(ExceptionUtil.getExceptionMessageChain(e)).append(StringUtils.LF);
		}

		return messages.toString();
	}

	private static class MethodCallVisitorContext {

		private ScriptContext scriptContext = new ScriptContext();

		private ScriptExpressionParser parser = new ScriptExpressionParser(scriptContext);

		String scriptShortClassName;

		List<Exception> exceptions = new LinkedList<>();

		Map<String, CompilationBlock> compiledExpressions = new LinkedHashMap<>();
	}

	private static class MethodCallVisitor extends VoidVisitorAdapter<MethodCallVisitorContext> {

		@Override
		public void visit(MethodCallExpr m, MethodCallVisitorContext visitorContext) {
			Expression[] methodParams = m.getArgs().toArray(new Expression[] {});
			Method[] parentMethods = CompilerConstants.SCRIPT_CLASS.getMethods();

			for (Method parentMethod : parentMethods) {
				if (m.getName().equals(parentMethod.getName())
						&& methodParams.length == parentMethod.getParameterTypes().length) {

					Var varAnnotation = parentMethod.getAnnotation(Var.class);
					if (varAnnotation != null) {
						processVariableDeclaration(methodParams, varAnnotation, visitorContext);
						break;
					}

					int paramIdx = 0;
					for (Annotation[] annotations : parentMethod.getParameterAnnotations()) {
						for (Annotation a : annotations) {
							if (a.annotationType() == ExprParam.class) {
								processExpression(methodParams[paramIdx], visitorContext);
							}
						}
						paramIdx++;
					}
					break;
				}
			}
		}

		private void processVariableDeclaration(Expression[] methodParams, Var varAnnotation,
				MethodCallVisitorContext visitorContext) {
			// Method with Var annotation must have at least two string
			// parameters: name and type
			String varName = ((StringLiteralExpr) methodParams[0]).getValue();
			String varType = ((StringLiteralExpr) methodParams[1]).getValue();

			ScriptContext scriptContext = visitorContext.scriptContext;

			// Variables are saved in the script context for the further
			// expressions parsing stage
			try {
				String javaType = ClassNameUtil.resolveJavaClassName(varType);
				if (varAnnotation.scope() == Var.Scope.LOCAL) {
					scriptContext.addLocalVar(varName, javaType);
				} else {
					scriptContext.addInputVar(varName, javaType);
				}
			} catch (ScriptException e) {
				visitorContext.exceptions.add(e);
			}
		}

		private void processExpression(Expression expression, MethodCallVisitorContext visitorContext) {
			if (StringLiteralExpr.class.isInstance(expression)) {
				String scriptExpr = ((StringLiteralExpr) expression).getValue();
				String javaExpr;
				byte[] byteCode;
				String className;
				Map<String, CompilationBlock> expressions = visitorContext.compiledExpressions;
				try {
					className = ClassNameUtil.resolveExpressionClassName(visitorContext.scriptShortClassName,
							scriptExpr);

					// Skip compilation of the same expression
					if (expressions.containsKey(className)) {
						return;
					}

					javaExpr = visitorContext.parser.parse(scriptExpr);
					byteCode = compileInvocationToBytecode(className, javaExpr);
				} catch (DynamicCodeException | ClassCompilationException e) {
					visitorContext.exceptions.add(e);
					return;
				}

				CompilationBlock cb = new CompilationBlock(className, byteCode, javaExpr);
				expressions.put(className, cb);
			}
		}

	}

}
