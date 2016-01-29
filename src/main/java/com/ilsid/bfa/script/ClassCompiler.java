package com.ilsid.bfa.script;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.ilsid.bfa.common.ExceptionUtil;
import com.ilsid.bfa.persistence.DynamicClassLoader;

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
 * Compiles classes in runtime.
 * 
 * @author illia.sydorovych
 *
 */
public class ClassCompiler {

	private static final String EXPRESSION_ERROR_MESSAGE_TEMPLATE = "Failed to compile the expression in the script [{}]";

	private static final CtClass[] NO_ARGS = {};

	private static final ClassPool classPool;

	private static Logger logger;

	static {
		classPool = ClassPool.getDefault();
		classPool.appendClassPath(new ClassClassPath(CompilerConstants.SCRIPT_CLASS));
	}

	/**
	 * Compiles a byte code for a class that implements {@link DynamicCodeInvocation} interface.
	 * 
	 * @param className
	 *            class name
	 * @param expression
	 *            java source code (implementation)
	 * @return a byte code for {@link DynamicCodeInvocation} implementation
	 * @throws ClassCompilationException
	 *             in case of compilation failure
	 */
	public static synchronized byte[] compileInvocation(String className, String expression)
			throws ClassCompilationException {
		byte[] result;

		try {
			CtClass clazz = buildInvocationClass(className, expression);
			result = toBytecode(clazz);
		} catch (NotFoundException | CannotCompileException | IOException e) {
			throw new ClassCompilationException(String.format(
					"Compilation of Invocation failed. Class [%s]. ValueExpression [%s]", className, expression), e);
		}

		return result;
	}

	/**
	 * Compiles a byte code for a class that extends {@link Script} class.
	 * 
	 * @param className
	 *            class name
	 * @param scriptBody
	 *            java source code (the contents of {@link Script#doExecute()} method)
	 * @return a byte code for {@link Script} descendant
	 * @throws ClassCompilationException
	 *             in case of compilation failure
	 */
	public static synchronized byte[] compileScript(String className, String scriptBody)
			throws ClassCompilationException {
		byte[] result;

		try {
			CtClass clazz = buildScriptClass(className, scriptBody);
			result = toBytecode(clazz);
		} catch (NotFoundException | CannotCompileException | IOException e) {

			throw new ClassCompilationException(String.format("Compilation of Script [%s] failed", className), e);
		}

		return result;
	}

	/**
	 * Compiles a byte code for an entity using the specified class name and body. The source code to compile will have
	 * the following format: </br>
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
	 * <code>type name[;type name[;type name[...]]]</code> </br>
	 * </br>
	 * The entity body example: </br>
	 * </br>
	 * <code>java.lang.Integer Days;java.lang.Integer ProlongDays;java.lang.Double MonthlyFee</code>
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
				String typeName = exprParts[0];
				String fieldName = exprParts[1];

				CtClass typeClass = classPool.get(typeName);
				CtField field = new CtField(typeClass, fieldName, clazz);
				field.setModifiers(Modifier.PUBLIC);
				clazz.addField(field);
			}

			result = toBytecode(clazz);

		} catch (NotFoundException | CannotCompileException | IOException | IllegalStateException e) {

			throw new ClassCompilationException(String.format("Compilation of Entity [%s] failed", className), e);

		} finally {
			classPool.removeClassPath(dynamicClassPath);
		}

		return result;
	}

	/**
	 * Compiles all expressions defined in the given script. The expression classes are defined in the script's package.
	 * 
	 * @param scriptSourceCode
	 *            the source code of the script class
	 * @return a collection of {@link CompilationBlock} instances for each expression
	 * @throws ClassCompilationException
	 *             if the compilation of any expression failed
	 * @throws IllegalArgumentException
	 *             in case of invalid source code
	 */
	public static Collection<CompilationBlock> compileScriptExpressions(String scriptSourceCode)
			throws ClassCompilationException, IllegalArgumentException {
		CompilationUnit compilationUnit;

		try {
			try (InputStream scriptSource = IOUtils
					.toInputStream(ScriptSourcePreprocessor.processVarargs(scriptSourceCode));) {
				compilationUnit = JavaParser.parse(scriptSource);
			}
		} catch (ParseException e) {
			throw new IllegalArgumentException("Invalid script source code", e);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to convert script source code into input stream", e);
		}

		PackageDeclarationVisitor pkgDeclarationVisitor = new PackageDeclarationVisitor();
		pkgDeclarationVisitor.visit(compilationUnit, null);

		ClassDeclarationVisitor classDeclarationVisitor = new ClassDeclarationVisitor();
		classDeclarationVisitor.visit(compilationUnit, null);

		MethodCallVisitorContext methodCallContext = new MethodCallVisitorContext();
		methodCallContext.packageName = pkgDeclarationVisitor.packageName;
		methodCallContext.scriptShortClassName = classDeclarationVisitor.shortClassName;

		new MethodCallVisitor().visit(compilationUnit, methodCallContext);

		List<Exception> exceptions = methodCallContext.exceptions;
		if (!exceptions.isEmpty()) {
			if (logger != null) {
				for (Exception e : exceptions) {
					logger.error(EXPRESSION_ERROR_MESSAGE_TEMPLATE, classDeclarationVisitor.shortClassName, e);
				}
			}

			throw new ClassCompilationException(
					String.format("Compilation of the script [%s] failed", classDeclarationVisitor.shortClassName)
							+ StringUtils.LF + mergeErrorMessages(exceptions));
		}

		return methodCallContext.compiledExpressions.values();
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

	private static ClassPool getClassPool() {
		return classPool;
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

		// Dynamic class loader is needed in case when the expression refers to the generated types
		final LoaderClassPath dynamicClassPath = new LoaderClassPath(DynamicClassLoader.getInstance());
		classPool.appendClassPath(dynamicClassPath);
		try {
			method = new CtMethod(classPool.get(Object.class.getName()), "invoke", NO_ARGS, clazz);
			method.setBody(expression);
			clazz.addMethod(method);
		} finally {
			classPool.removeClassPath(dynamicClassPath);
		}

		return clazz;
	}

	private static CtClass buildScriptClass(String className, String scriptBody)
			throws NotFoundException, CannotCompileException, IOException {
		ClassPool classPool = getClassPool();
		CtClass clazz = classPool.makeClass(className);
		clazz.setSuperclass(classPool.get(CompilerConstants.SCRIPT_CLASS_NAME));

		CtConstructor cons = new CtConstructor(NO_ARGS, clazz);
		cons.setBody(";");
		clazz.addConstructor(cons);

		CtMethod method = new CtMethod(CtClass.voidType, "doExecute", NO_ARGS, clazz);
		method.setModifiers(Modifier.PROTECTED);

		String compilableScriptBody = ScriptSourcePreprocessor.processVarargs(scriptBody);
		String body = "{" + StringUtils.LF + compilableScriptBody + StringUtils.LF + "}";
		method.setBody(body);
		clazz.addMethod(method);

		return clazz;
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

	private static class MethodCallVisitorContext {

		private ScriptContext scriptContext = new ScriptContext();

		private ScriptExpressionParser parser = new ScriptExpressionParser(scriptContext);

		String packageName;

		String scriptShortClassName;

		List<Exception> exceptions = new LinkedList<>();

		Map<String, CompilationBlock> compiledExpressions = new LinkedHashMap<>();
	}

	private static class MethodCallVisitor extends VoidVisitorAdapter<MethodCallVisitorContext> {

		private static final String DOT = ".";

		@Override
		public void visit(MethodCallExpr m, MethodCallVisitorContext visitorContext) {
			for (Node child : m.getChildrenNodes()) {
				child.accept(this, visitorContext);
			}

			Expression[] methodParams = m.getArgs().toArray(new Expression[] {});
			List<Method> parentMethods = new LinkedList<>();
			parentMethods.addAll(Arrays.asList(Script.class.getMethods()));
			parentMethods.addAll(Arrays.asList(Script.ActionResult.class.getMethods()));

			for (Method parentMethod : parentMethods) {
				if (m.getName().equals(parentMethod.getName())
						&& methodParams.length == parentMethod.getParameterTypes().length) {

					Var varAnnotation = parentMethod.getAnnotation(Var.class);
					if (varAnnotation != null) {
						processVariableDeclaration(methodParams, varAnnotation, visitorContext);
					}

					int paramIdx = 0;
					for (Annotation[] annotations : parentMethod.getParameterAnnotations()) {
						for (Annotation a : annotations) {
							if (a.annotationType() == ExprParam.class) {
								processExpression(methodParams[paramIdx], visitorContext, ((ExprParam) a).compile());
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
				String javaType = TypeNameResolver.resolveEntityClassName(varType);
				if (varAnnotation.scope() == Var.Scope.LOCAL) {
					scriptContext.addLocalVar(varName, javaType);
				} else {
					scriptContext.addInputVar(varName, javaType, null);
				}
			} catch (ScriptException e) {
				visitorContext.exceptions.add(e);
			}
		}

		private void processExpression(Node expression, MethodCallVisitorContext visitorContext,
				boolean compilationIsNeeded) {
			if (StringLiteralExpr.class.isInstance(expression)) {
				String scriptExpr = ((StringLiteralExpr) expression).getValue();
				String javaExpr;
				byte[] byteCode;
				String className;
				Map<String, CompilationBlock> expressions = visitorContext.compiledExpressions;
				try {
					className = new StringBuilder(visitorContext.packageName).append(DOT)
							.append(visitorContext.scriptShortClassName)
							.append(TypeNameResolver.resolveExpressionClassNamePart(scriptExpr)).toString();

					// Skip parsing and compilation of the same expression
					if (expressions.containsKey(className)) {
						return;
					}

					javaExpr = visitorContext.parser.parse(scriptExpr);

					if (compilationIsNeeded) {
						byteCode = compileInvocation(className, javaExpr);
						CompilationBlock cb = new CompilationBlock(className, byteCode, javaExpr);
						expressions.put(className, cb);
					}
				} catch (ParsingException | ClassCompilationException e) {
					visitorContext.exceptions.add(e);
				}
			} else if (ArrayCreationExpr.class.isInstance(expression)) {
				// This is a case for varargs that have been replaced with explicit array statement
				for (Node childNode : expression.getChildrenNodes()) {
					for (Node grandChildNode : childNode.getChildrenNodes()) {
						// The string literals with method parameter values are the grand-children of "Create Array"
						// expression
						processExpression(grandChildNode, visitorContext, true);
					}
				}
			}
		}

	}

	private static class PackageDeclarationVisitor extends VoidVisitorAdapter<Void> {

		String packageName;

		@Override
		public void visit(final PackageDeclaration pkgDeclaration, final Void context) {
			packageName = pkgDeclaration.getName().toString();
		}
	}

	private static class ClassDeclarationVisitor extends VoidVisitorAdapter<Void> {

		String shortClassName;

		@Override
		public void visit(final ClassOrInterfaceDeclaration clsDeclaration, final Void context) {
			shortClassName = clsDeclaration.getName();
		}
	}

}
