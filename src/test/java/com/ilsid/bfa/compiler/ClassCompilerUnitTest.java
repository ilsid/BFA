package com.ilsid.bfa.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.script.DynamicCodeInvocation;
import com.ilsid.bfa.script.Script;
import com.ilsid.bfa.script.ScriptContext;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;

public class ClassCompilerUnitTest extends BaseUnitTestCase {

	private static final String SCRIPTS_DIR = "src/test/resources/dynamicCode/";

	private static final String BYTECODE_DIR = "src/test/resources/byteCode/";

	private static final String TEST_INVOCATION_EXPRESSION = "return Integer.valueOf(2 - 1);";

	private static final String TEST_INVOCATION_CLASS_NAME = "com.ilsid.bfa.test.generated.TestInvocation";

	private static final String TEST_INVOCATION_CLASS_NAME_2 = "com.ilsid.bfa.test.generated.TestInvocation2";

	private static final String TEST_SCRIPT_CLASS_NAME = "com.ilsid.bfa.test.generated.TestScript";

	private static final String TEST_SCRIPT_CLASS_NAME_2 = "com.ilsid.bfa.test.generated.TestScript2";

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
	public void compileExpressionsSanityCheck2() throws Exception {
		StringBuilder sourceCode = new StringBuilder();
		try (InputStream scriptBody = loadScript("single-expression-script.txt");) {
			sourceCode.append("package com.ilsid.bfa.generated.script;").append("\n");
			sourceCode.append("import com.ilsid.bfa.script.Script;").append("\n");
			sourceCode.append("import com.ilsid.bfa.script.ScriptException;").append("\n");
			sourceCode.append("public class TestScript33 extends Script {").append("\n");
			sourceCode.append("protected void doExecute() throws ScriptException {").append("\n");
			sourceCode.append(IOUtils.toString(scriptBody, "UTF-8")).append("\n");
			sourceCode.append("}").append("\n");
			sourceCode.append("}");
		}
		InputStream scriptSource = IOUtils.toInputStream(sourceCode.toString(), "UTF-8");
		ClassCompiler.compileScriptExpressions("com.ilsid.bfa.generated.script.TestScript33", scriptSource);
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

}
