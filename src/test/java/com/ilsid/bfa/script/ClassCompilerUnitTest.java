package com.ilsid.bfa.script;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.filesystem.FSCodeRepository;

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

	private static final String SCRIPT_PACKAGE = "com.ilsid.bfa.generated.script.default_group";

	private ScriptContext mockContext;

	@BeforeClass
	@SuppressWarnings("serial")
	public static void beforeClass() throws Exception {
		CodeRepository repository = new FSCodeRepository();
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", TestConstants.TEST_RESOURCES_DIR + "/code_repository");
			}
		});

		DynamicClassLoader.setRepository(repository);
	}

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
		CompilationBlock[] expressions = compileScriptExpressions("TestScript33", "single-expression-script.txt");

		assertEquals(1, expressions.length);

		String exprClassName = expressions[0].getClassName();
		assertExpressionShortClassName("TestScript33$$2", exprClassName);

		DynamicCodeInvocation expr = (DynamicCodeInvocation) loadFromBytecode(exprClassName,
				expressions[0].getByteCode()).newInstance();

		Integer exprResult = (Integer) expr.invoke();
		assertEquals(2, exprResult);
	}

	@Test
	public void nothingIsCompiledInScriptWithNoExpressions() throws Exception {
		CompilationBlock[] expressions = compileScriptExpressions("TestScript33", "declarations-only-script.txt");
		assertEquals(0, expressions.length);
	}
	
	@Test
	public void nothingIsCompiledInScriptWithNoExpressions2() throws Exception {
		// The script contains two expressions, but only one expression is compiled
		CompilationBlock[] expressions = compileScriptExpressions("TestScript77", "one-noncompiled-expression-script.txt");

		assertEquals(1, expressions.length);
		
		String exprName = expressions[0].getClassName();
		assertExpressionShortClassName("TestScript77$$1", exprName);
	}

	@Test
	public void onlyUniqueScriptExpressionsAreCompiled() throws Exception {
		// The script contains two "Var1 - Var2" expressions, but only a single
		// compiled expression is returned
		CompilationBlock[] expressions = compileScriptExpressions("TestScript44", "duplicated-expression-script.txt");

		assertEquals(3, expressions.length);

		String exprName1 = expressions[0].getClassName();
		String exprName2 = expressions[1].getClassName();
		String exprName3 = expressions[2].getClassName();

		assertExpressionShortClassName("TestScript44$$2", exprName1);
		assertExpressionShortClassName("TestScript44$$1", exprName2);
		assertExpressionShortClassName("TestScript44$$Var1_Mns_Var2", exprName3);

		DynamicCodeInvocation expr1 = (DynamicCodeInvocation) loadFromBytecode(exprName1, expressions[0].getByteCode())
				.newInstance();
		DynamicCodeInvocation expr2 = (DynamicCodeInvocation) loadFromBytecode(exprName2, expressions[1].getByteCode())
				.newInstance();
		DynamicCodeInvocation expr3 = (DynamicCodeInvocation) loadFromBytecode(exprName3, expressions[2].getByteCode())
				.newInstance();

		// Define the variables declared in the script. They are needed for the expression runtime.
		expr3.setScriptContext(
				ScriptContextUtil.createContext(new Variable("Var1", "Number", 2), new Variable("Var2", "Number", 1)));

		Integer exprResult1 = (Integer) expr1.invoke();
		Integer exprResult2 = (Integer) expr2.invoke();
		Integer exprResult3 = (Integer) expr3.invoke();
		// Var1 = 2
		assertEquals(2, exprResult1);
		// Var2 = 1
		assertEquals(1, exprResult2);
		// Var2 - Var1 = 1
		assertEquals(1, exprResult3);
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
		msg.append("Could not parse expression [Var55]: Unexpected token [Var55]").append(StringUtils.LF);
		msg.append("   Caused by: Unexpected token [Var55]").append(StringUtils.LF);
		msg.append(
				"Could not parse expression [Var1 - Var33]: Integer value or variable is expected after operand [-], but was [Var33]")
				.append(StringUtils.LF);
		msg.append("   Caused by: Integer value or variable is expected after operand [-], but was [Var33]")
				.append(StringUtils.LF);
		msg.append("Could not parse expression [abc]: Unexpected token [abc]").append(StringUtils.LF);
		msg.append("   Caused by: Unexpected token [abc]").append(StringUtils.LF);
		exceptionRule.expectMessage(msg.toString());

		compileScriptExpressions("TestScript33", "three-invalid-expressions-script.txt");
	}

	@Test
	public void entityWithSingleFieldOfPredefinedTypeCanBeCompiledToBytecode() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity01";
		byte[] byteCode = ClassCompiler.compileEntityToBytecode(className, "java.lang.Integer testField");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(1, clazz.getFields().length);
		assertSame(Integer.class, clazz.getField("testField").getType());
	}

	@Test
	public void entityWithSeveralFieldsOfPredefinedTypeCanBeCompiledToBytecode() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity02";
		byte[] byteCode = ClassCompiler.compileEntityToBytecode(className,
				"java.lang.Integer testField1; java.lang.Double testField2; java.lang.Integer testField3");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(3, clazz.getFields().length);
		assertSame(Integer.class, clazz.getField("testField1").getType());
		assertSame(Double.class, clazz.getField("testField2").getType());
		assertSame(Integer.class, clazz.getField("testField3").getType());
	}

	@Test
	public void entityWithSingleFieldOfGeneratedTypeCanBeCompiledToBytecode() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity03";
		String fieldTypeName = "com.ilsid.bfa.generated.compilertest.GeneratedContract";

		byte[] byteCode = ClassCompiler.compileEntityToBytecode(className, fieldTypeName + " contract");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(1, clazz.getFields().length);
		assertEquals(fieldTypeName, clazz.getField("contract").getType().getName());
	}

	@Test
	public void entityWithTwoFieldsOfSameGeneratedTypeCanBeCompiledToBytecode() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity04";
		String fieldTypeName = "com.ilsid.bfa.generated.compilertest.GeneratedContract";

		byte[] byteCode = ClassCompiler.compileEntityToBytecode(className,
				fieldTypeName + " contract1;" + fieldTypeName + " contract2;");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(2, clazz.getFields().length);
		assertEquals(fieldTypeName, clazz.getField("contract1").getType().getName());
		assertEquals(fieldTypeName, clazz.getField("contract2").getType().getName());
	}

	@Test
	public void entityWithMultipleFieldsOfGeneratedTypesCanBeCompiledToBytecode() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity05";
		String fieldTypeName1 = "com.ilsid.bfa.generated.compilertest.GeneratedContract";
		String fieldTypeName2 = "com.ilsid.bfa.generated.compilertest.AnotherGeneratedContract";

		byte[] byteCode = ClassCompiler.compileEntityToBytecode(className,
				fieldTypeName1 + " contract1;" + fieldTypeName2 + " contract2;");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(2, clazz.getFields().length);
		assertEquals(fieldTypeName1, clazz.getField("contract1").getType().getName());
		assertEquals(fieldTypeName2, clazz.getField("contract2").getType().getName());
	}

	@Test
	public void entityWithFieldsOfPredefinedAndGeneratedTypesCanBeCompiledToBytecode() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity06";
		String fieldTypeName1 = "com.ilsid.bfa.generated.compilertest.GeneratedContract";
		String fieldTypeName2 = "java.lang.Integer";

		byte[] byteCode = ClassCompiler.compileEntityToBytecode(className,
				fieldTypeName1 + " contract;" + fieldTypeName2 + " days;");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(2, clazz.getFields().length);
		assertEquals(fieldTypeName1, clazz.getField("contract").getType().getName());
		assertEquals(fieldTypeName2, clazz.getField("days").getType().getName());
	}

	@Test
	public void entityWithInvalidBodyCanNotBeCompiled() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		exceptionRule.expectMessage(
				"Compilation of Entity [com.ilsid.bfa.test.generated.entity.Entity07] failed. Expression [java.lang.Integer field2 field3] is invalid");

		String className = "com.ilsid.bfa.test.generated.entity.Entity07";
		ClassCompiler.compileEntityToBytecode(className, "java.lang.Double field1; java.lang.Integer field2 field3;");
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
		String sourceCode = String.format(CompilerConstants.SCRIPT_SOURCE_TEMPLATE, shortClassName.toLowerCase(),
				shortClassName, body);

		InputStream scriptSource = IOUtils.toInputStream(sourceCode.toString(), "UTF-8");
		Collection<CompilationBlock> expressions = ClassCompiler.compileScriptExpressions(scriptSource);

		return expressions.toArray(new CompilationBlock[] {});
	}

	private void assertExpressionShortClassName(String expected, String actual) {
		String scriptChildPackage = expected.substring(0, expected.lastIndexOf("$$")).toLowerCase();
		assertEquals(SCRIPT_PACKAGE + "." + scriptChildPackage + "." + expected, actual);
	}

	private Expectations getScriptExpectations() throws Exception {
		return new Expectations() {
			{
				oneOf(mockContext).addLocalVar("Var1", "java.lang.Integer");
				oneOf(mockContext).addLocalVar("Var2", "java.lang.Double");
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
