package com.ilsid.bfa.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.script.DynamicCodeInvocation;
import com.ilsid.bfa.script.Script;
import com.ilsid.bfa.script.ScriptContext;

public class ClassCompilerUnitTest extends BaseUnitTestCase {

	private static final String SCRIPTS_DIR = "src/test/resources/dynamicCode/";

	private static final String BYTECODE_DIR = "src/test/resources/byteCode/";

	private static final String TEST_INVOCATION_EXPRESSION = "return Integer.valueOf(2 - 1);";

	private static final String TEST_INVOCATION_CLASS_NAME = "com.ilsid.bfa.test.generated.TestInvocation";

	private static final String TEST_INVOCATION_CLASS_NAME_2 = "com.ilsid.bfa.test.generated.TestInvocation2";

	private static final String TEST_SCRIPT_CLASS_NAME = "com.ilsid.bfa.test.generated.TestScript";

	private static final String TEST_SCRIPT_CLASS_NAME_2 = "com.ilsid.bfa.test.generated.TestScript2";

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
		try (InputStream body = loadScript("declarations-only-script.txt");) {
			Class<Script> clazz = (Class<Script>) ClassCompiler.compileScript(TEST_SCRIPT_CLASS_NAME, body);
			Script script = clazz.newInstance();
			setInaccessibleParentField(script, "scriptContext", mockContext);

			checking(new Expectations() {
				{
					oneOf(mockContext).addLocalVar("Var1", "Integer");
					oneOf(mockContext).addLocalVar("Var2", "Double");
				}
			});

			script.execute();
		}
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
	public void classCanBeLoadedFromBytecode() throws Exception {
		byte[] byteCode = loadClass("DummyClass.class");

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

		byte[] byteCode = loadClass("DummyClass2.class");

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
		assertExpressionShortClassName("TestScript33$$Var1_Mns_Var2", expressions[0].getClassName());
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
		CompilationBlock[] expressions = compileScriptExpressions("TestScript33", "duplicated-expression-script.txt");
		assertEquals(2, expressions.length);
		assertExpressionShortClassName("TestScript33$$Var1_Mns_Var2", expressions[0].getClassName());
		assertExpressionShortClassName("TestScript33$$1", expressions[1].getClassName());
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
	public void errorDetailsAreProvidedIfScriptContainsMultipleInvalidExpression() throws Exception {
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
		try (InputStream body = loadScript("declarations-only-script.txt");) {
			Class<?> clazz = ClassCompiler.compileScript(className, body);
		}
	}

	private InputStream loadScript(String fileName) throws Exception {
		return new FileInputStream(new File(SCRIPTS_DIR + fileName));
	}

	private byte[] loadClass(String fileName) throws Exception {
		byte[] result;

		try (InputStream is = new FileInputStream(BYTECODE_DIR + fileName)) {
			result = IOUtils.toByteArray(is);
		}

		return result;
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
		StringBuilder sourceCode = new StringBuilder();
		try (InputStream scriptBody = loadScript(fileName);) {
			sourceCode.append("package ").append(SCRIPT_PACKAGE).append(";\n");
			sourceCode.append("import com.ilsid.bfa.script.Script;").append("\n");
			sourceCode.append("import com.ilsid.bfa.script.ScriptException;").append("\n");
			sourceCode.append("public class ").append(shortClassName).append(" extends Script {").append("\n");
			sourceCode.append("protected void doExecute() throws ScriptException {").append("\n");
			sourceCode.append(IOUtils.toString(scriptBody, "UTF-8")).append("\n");
			sourceCode.append("}").append("\n");
			sourceCode.append("}");
		}
		InputStream scriptSource = IOUtils.toInputStream(sourceCode.toString(), "UTF-8");
		Collection<CompilationBlock> expressions = ClassCompiler.compileScriptExpressions(shortClassName, scriptSource);

		return expressions.toArray(new CompilationBlock[] {});
	}

	private void assertExpressionShortClassName(String expected, String actual) {
		assertEquals(SCRIPT_PACKAGE + "." + expected, actual);
	}

}
