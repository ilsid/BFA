package com.ilsid.bfa.script;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.common.IOHelper;

import javassist.ByteArrayClassPath;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;

public class ClassCompilerUnitTest extends BaseUnitTestCase {

	private static final String TEST_INVOCATION_EXPRESSION = "return Integer.valueOf(2 - 1);";

	private static final String TEST_INVOCATION_CLASS_NAME = "com.ilsid.bfa.test.generated.TestInvocation";

	private static final String TEST_INVOCATION_CLASS_NAME_2 = "com.ilsid.bfa.test.generated.TestInvocation2";

	private static final String TEST_INVOCATION_CLASS_NAME_3 = "com.ilsid.bfa.test.generated.TestInvocation3";

	private static final String TEST_SCRIPT_CLASS_NAME = "com.ilsid.bfa.test.generated.TestScript";

	private static final String TEST_SCRIPT_CLASS_NAME_2 = "com.ilsid.bfa.test.generated.TestScript2";

	private static final String TEST_SCRIPT_CLASS_NAME_3 = "com.ilsid.bfa.test.generated.TestScript3";

	private static final String SCRIPT_PACKAGE = "com.ilsid.bfa.generated.script";

	private ScriptContext mockContext;

	@Before
	public void setUp() throws Exception {
		mockContext = mock(ScriptContext.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void invocationCanBeCompiled() throws Exception {
		Class<DynamicCodeInvocation> clazz = (Class<DynamicCodeInvocation>) ClassCompiler
				.compileInvocation(TEST_INVOCATION_CLASS_NAME, TEST_INVOCATION_EXPRESSION);

		DynamicCodeInvocation instance = clazz.newInstance();
		assertEquals(new Integer(1), instance.invoke());
	}

	@Test
	@SuppressWarnings("unused")
	public void invocationWithSameNameCanNotBeCompiledTwice() throws Exception {
		// Note, we must use unique class name in this test to avoid conflicts
		// with classes already loaded into JVM by other tests.
		exceptionRule.expect(ClassCompilationException.class);
		exceptionRule.expectMessage(
				"Compilation of Invocation class failed. Class [com.ilsid.bfa.test.generated.TestInvocation2]. ValueExpression [return Integer.valueOf(2 - 1);]");

		Class<?> clazz;
		clazz = ClassCompiler.compileInvocation(TEST_INVOCATION_CLASS_NAME_2, TEST_INVOCATION_EXPRESSION);
		clazz = ClassCompiler.compileInvocation(TEST_INVOCATION_CLASS_NAME_2, TEST_INVOCATION_EXPRESSION);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void scriptCanBeCompiled() throws Exception {
		String body = IOHelper.loadScript("declarations-only-script.txt");
		Class<Script> clazz = (Class<Script>) ClassCompiler.compileScript(TEST_SCRIPT_CLASS_NAME, body);
		Script script = clazz.newInstance();
		setInaccessibleParentField(script, "scriptContext", mockContext);
		checking(getScriptExpectations());
		script.execute();
	}

	@Test
	public void scriptWithSameNameCanNotBeCompiledTwice() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		exceptionRule.expectMessage("Compilation of Script class [com.ilsid.bfa.test.generated.TestScript2] failed");

		// Note, we must use unique class name in this test to avoid conflicts
		// with classes already loaded into JVM by other tests.
		compileScript(TEST_SCRIPT_CLASS_NAME_2);
		compileScript(TEST_SCRIPT_CLASS_NAME_2);
	}

	@Test
	public void scriptCanBeCompiledToBytecode() throws Exception {
		String body = IOHelper.loadScript("declarations-only-script.txt");
		byte[] byteCode = ClassCompiler.compileScriptToBytecode(TEST_SCRIPT_CLASS_NAME_3, body);
		Script script = (Script) loadFromBytecode(TEST_SCRIPT_CLASS_NAME_3, byteCode).newInstance();
		setInaccessibleParentField(script, "scriptContext", mockContext);
		checking(getScriptExpectations());
		script.execute();
	}

	@Test
	public void scriptWithSameNameCanBeCompiledToBytecodeMultipleTimes() {
		try {
			compileScriptToBytecode(TEST_SCRIPT_CLASS_NAME_3);
			compileScriptToBytecode(TEST_SCRIPT_CLASS_NAME_3);
			compileScriptToBytecode(TEST_SCRIPT_CLASS_NAME_3);
		} catch (Exception e) {
			failCausedByUnexpectedException(e);
		}
	}

	@Test
	public void invocationCanBeCompiledToBytecode() throws Exception {
		byte[] byteCode = ClassCompiler.compileInvocationToBytecode(TEST_INVOCATION_CLASS_NAME_3,
				TEST_INVOCATION_EXPRESSION);

		DynamicCodeInvocation instance = (DynamicCodeInvocation) loadFromBytecode(TEST_INVOCATION_CLASS_NAME_3,
				byteCode).newInstance();
		assertEquals(new Integer(1), instance.invoke());
	}

	@Test
	public void invocationWithSameNameCanBeCompiledToBytecodeMultipleTimes() {
		try {
			ClassCompiler.compileInvocationToBytecode(TEST_INVOCATION_CLASS_NAME_3, TEST_INVOCATION_EXPRESSION);
			ClassCompiler.compileInvocationToBytecode(TEST_INVOCATION_CLASS_NAME_3, TEST_INVOCATION_EXPRESSION);
			ClassCompiler.compileInvocationToBytecode(TEST_INVOCATION_CLASS_NAME_3, TEST_INVOCATION_EXPRESSION);
		} catch (ClassCompilationException e) {
			failCausedByUnexpectedException(e);
		}
	}

	@Test
	public void classCanBeLoadedFromBytecode() throws Exception {
		byte[] byteCode = IOHelper.loadClass("DummyClass.class");

		Class<?> dummyClass = ClassCompiler.loadFromBytecode("com.ilsid.bfa.test.types.DummyClass", byteCode);
		Object dummy = dummyClass.newInstance();

		assertNull(invokeMethod(dummy, "getValue"));
		String value = "some value";
		invokeMethod(dummy, "setValue", value);
		assertEquals(value, invokeMethod(dummy, "getValue"));
	}

	@Test
	@SuppressWarnings("unused")
	public void classCanNotBeLoadedTwiceFromBytecode() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		exceptionRule.expectMessage("Failed to create class com.ilsid.bfa.test.types.DummyClass2 from byte code");

		byte[] byteCode = IOHelper.loadClass("DummyClass2.class");

		Class<?> dummyClass;
		// Note, we must use unique class name in this test to avoid conflicts
		// with classes already loaded into JVM by other tests.
		String className = "com.ilsid.bfa.test.types.DummyClass2";
		dummyClass = ClassCompiler.loadFromBytecode(className, byteCode);
		dummyClass = ClassCompiler.loadFromBytecode(className, byteCode);
	}

	@Test
	public void singleScriptExpressionCanBeCompiled() throws Exception {
		// The script contains two "Var1 - Var2" expressions, but only a single
		// compiled expression is returned
		CompilationBlock[] expressions = compileScriptExpressions("TestScript33", "single-expression-script.txt");

		assertEquals(1, expressions.length);

		String exprClassName = expressions[0].getClassName();
		assertExpressionShortClassName("TestScript33$$Var1_Mns_Var2", exprClassName);

		DynamicCodeInvocation expr = (DynamicCodeInvocation) loadFromBytecode(exprClassName,
				expressions[0].getByteCode()).newInstance();
		// Define the variables declared in the script. They are needed for the
		// expression runtime.
		expr.setScriptContext(
				ScriptContextUtil.createContext(new Variable("Var1", "Number", 2), new Variable("Var2", "Number", 1)));

		Integer exprResult = (Integer) expr.invoke();
		assertEquals(1, exprResult);
	}

	@Test
	public void nothingIsCompiledInScriptWithNoExpressions() throws Exception {
		CompilationBlock[] expressions = compileScriptExpressions("TestScript33", "declarations-only-script.txt");
		assertEquals(0, expressions.length);
	}

	@Test
	public void onlyUniqueScriptExpressionsAreCompiled() throws Exception {
		// The script contains two "Var1 - Var2" expressions, but only a single
		// compiled expression is returned
		CompilationBlock[] expressions = compileScriptExpressions("TestScript44", "duplicated-expression-script.txt");

		assertEquals(2, expressions.length);

		String exprName1 = expressions[0].getClassName();
		String exprName2 = expressions[1].getClassName();

		assertExpressionShortClassName("TestScript44$$Var1_Mns_Var2", exprName1);
		assertExpressionShortClassName("TestScript44$$1", exprName2);

		DynamicCodeInvocation expr1 = (DynamicCodeInvocation) loadFromBytecode(exprName1, expressions[0].getByteCode())
				.newInstance();
		DynamicCodeInvocation expr2 = (DynamicCodeInvocation) loadFromBytecode(exprName2, expressions[1].getByteCode())
				.newInstance();

		// Define the variables declared in the script. They are needed for the
		// expression runtime.
		expr1.setScriptContext(
				ScriptContextUtil.createContext(new Variable("Var1", "Number", 2), new Variable("Var2", "Number", 1)));

		Integer exprResult1 = (Integer) expr1.invoke();
		Integer exprResult2 = (Integer) expr2.invoke();
		assertEquals(1, exprResult1);
		assertEquals(1, exprResult2);
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsInvalidExpression() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		StringBuilder msg = new StringBuilder();
		msg.append("Compilation of expressions in script [TestScript33] failed").append(StringUtils.LF);
		msg.append(
				"Could not parse expression [Var1 - Var33]: Integer value or variable is expected after operand [-], but was [Var33]")
				.append(StringUtils.LF);
		msg.append("   Caused by: Integer value or variable is expected after operand [-], but was [Var33]");
		exceptionRule.expectMessage(msg.toString());

		compileScriptExpressions("TestScript33", "one-invalid-expression-script.txt");
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsMultipleInvalidExpressions() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		StringBuilder msg = new StringBuilder();
		msg.append("Compilation of expressions in script [TestScript33] failed").append(StringUtils.LF);
		msg.append(
				"Could not parse expression [Var1 - Var33]: Integer value or variable is expected after operand [-], but was [Var33]")
				.append(StringUtils.LF);
		msg.append("   Caused by: Integer value or variable is expected after operand [-], but was [Var33]")
				.append(StringUtils.LF);
		msg.append("Could not parse expression [abc]: Unexpected token [abc]").append(StringUtils.LF);
		msg.append("   Caused by: Unexpected token [abc]").append(StringUtils.LF);
		exceptionRule.expectMessage(msg.toString());

		compileScriptExpressions("TestScript33", "two-invalid-expressions-script.txt");
	}

	@SuppressWarnings("unused")
	private void compileScript(String className) throws Exception {
		Class<?> clazz = ClassCompiler.compileScript(className, IOHelper.loadScript("declarations-only-script.txt"));
	}

	@SuppressWarnings("unused")
	private void compileScriptToBytecode(String className) throws Exception {
		byte[] byteCode = ClassCompiler.compileScriptToBytecode(className,
				IOHelper.loadScript("declarations-only-script.txt"));
	}

	private String invokeMethod(Object target, String name) throws Exception {
		Method method = target.getClass().getDeclaredMethod(name);
		return (String) method.invoke(target);
	}

	private void invokeMethod(Object target, String name, String value) throws Exception {
		Method method = target.getClass().getDeclaredMethod(name, String.class);
		method.invoke(target, value);
	}

	private CompilationBlock[] compileScriptExpressions(String shortClassName, String fileName) throws Exception {
		String body = IOHelper.loadScript(fileName);
		String sourceCode = String.format(CompilerConstants.SCRIPT_SOURCE_TEMPLATE, shortClassName, body);

		InputStream scriptSource = IOUtils.toInputStream(sourceCode.toString(), "UTF-8");
		Collection<CompilationBlock> expressions = ClassCompiler.compileScriptExpressions(scriptSource);

		return expressions.toArray(new CompilationBlock[] {});
	}

	private void assertExpressionShortClassName(String expected, String actual) {
		assertEquals(SCRIPT_PACKAGE + "." + expected, actual);
	}

	private Expectations getScriptExpectations() throws Exception {
		return new Expectations() {
			{
				oneOf(mockContext).addLocalVar("Var1", "Integer");
				oneOf(mockContext).addLocalVar("Var2", "Double");
			}
		};
	}

	private Class<?> loadFromBytecode(String className, byte[] byteCode) throws Exception {
		ClassPath classPathEntry = new ByteArrayClassPath(className, byteCode);
		ClassPool classPool = ClassPool.getDefault();
		classPool.appendClassPath(classPathEntry);
		CtClass clazz = classPool.get(className);
		Class<?> scriptClass = clazz.toClass();
		clazz.detach();
		classPool.removeClassPath(classPathEntry);

		return scriptClass;
	}

	private void failCausedByUnexpectedException(Exception e) {
		fail("Unexpected exception was thrown: " + e.getMessage());
	}

}
