package com.ilsid.bfa.script;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jmock.Expectations;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.script.ClassCompiler.ScriptCompilationUnit;

import javassist.ByteArrayClassPath;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;

public class ClassCompilerUnitTest extends BaseUnitTestCase {

	private static final String SCRIPT_CONTEXT_FIELD_NAME = "scriptContext";

	private static final String TEST_SCRIPT_CLASS_NAME = "com.ilsid.bfa.test.generated.TestScript";

	private ScriptContext mockContext;

	@BeforeClass
	public static void beforeClass() throws Exception {
		ScriptingRepositoryInitializer.init();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		ScriptingRepositoryInitializer.cleanup();
	}

	@Before
	public void setUp() throws Exception {
		mockContext = mock(ScriptContext.class);
	}

	@Test
	public void scriptCanBeCompiled() throws Exception {
		String body = IOHelper.loadScript("declarations-only-script.txt");
		byte[] byteCode = ClassCompiler.compileScript(TEST_SCRIPT_CLASS_NAME, body).getByteCode();
		Script script = (Script) loadFromBytecode(TEST_SCRIPT_CLASS_NAME, byteCode).newInstance();
		setInaccessibleParentField(script, SCRIPT_CONTEXT_FIELD_NAME, mockContext);
		checking(getScriptExpectations());
		script.execute();
	}

	@Test
	public void scriptWithSameNameCanBeCompiledMultipleTimes() {
		try {
			compileScript(TEST_SCRIPT_CLASS_NAME);
			compileScript(TEST_SCRIPT_CLASS_NAME);
			compileScript(TEST_SCRIPT_CLASS_NAME);
		} catch (Exception e) {
			failCausedByUnexpectedException(e);
		}
	}

	@Test
	public void scriptWithSingleExpressionCanBeCompiled() throws Exception {
		final String scriptClassName = "scriptWithSingleExpressionCanBeCompiled.TestScript33";
		ScriptCompilationUnit scriptUnit = compileScript(scriptClassName, "single-expression-script.txt");
		assertTrue(scriptUnit.getInputParameters().isEmpty());
		assertTrue(scriptUnit.getGeneratedSource().length() > 0);

		Script script = (Script) loadFromBytecode(scriptClassName, scriptUnit.getByteCode()).newInstance();
		setInaccessibleParentField(script, SCRIPT_CONTEXT_FIELD_NAME, mockContext);

		checking(new Expectations() {
			{
				// Expression "2" is replaced with value 2
				oneOf(mockContext).addLocalVar("Var1", "java.lang.Integer", 2);
			}
		});

		script.execute();
	}

	@Test
	public void scriptWithSeveralExpressionsCanBeCompiled() throws Exception {
		final String scriptClassName = "scriptWithSeveralExpressionsCanBeCompiled.TestScript44";
		ScriptCompilationUnit scriptUnit = compileScript(scriptClassName, "several-expressions-script.txt");
		assertTrue(scriptUnit.getInputParameters().isEmpty());
		assertTrue(scriptUnit.getGeneratedSource().length() > 0);

		Script script = (Script) loadFromBytecode(scriptClassName, scriptUnit.getByteCode()).newInstance();
		script.execute();
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsInvalidExpression() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		StringBuilder msg = new StringBuilder();
		msg.append("Script [TestScript33] contains errors. Compilation failed").append(StringUtils.LF);
		msg.append(
				"Could not parse expression [Var1 - Var33]: Number value or variable is expected after operand [-], but was [Var33]")
				.append(StringUtils.LF);
		msg.append("   Caused by: Number value or variable is expected after operand [-], but was [Var33]")
				.append(StringUtils.LF);
		exceptionRule.expectMessage(msg.toString());

		compileScript("TestScript33", "one-invalid-expression-script.txt");
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsMultipleInvalidExpressions() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		StringBuilder msg = new StringBuilder();
		msg.append("Script [TestScript33] contains errors. Compilation failed").append(StringUtils.LF);
		msg.append("Could not parse expression [Var55]: Unexpected token [Var55]").append(StringUtils.LF);
		msg.append("   Caused by: Unexpected token [Var55]").append(StringUtils.LF);
		msg.append(
				"Could not parse expression [Var1 - Var33]: Number value or variable is expected after operand [-], but was [Var33]")
				.append(StringUtils.LF);
		msg.append("   Caused by: Number value or variable is expected after operand [-], but was [Var33]")
				.append(StringUtils.LF);
		msg.append("Could not parse expression [abc]: Unexpected token [abc]").append(StringUtils.LF);
		msg.append("   Caused by: Unexpected token [abc]").append(StringUtils.LF);
		exceptionRule.expectMessage(msg.toString());

		compileScript("TestScript33", "three-invalid-expressions-script.txt");
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsInvalidEntityType() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		StringBuilder msg = new StringBuilder();
		msg.append("Script [TestScript33] contains errors. Compilation failed").append(StringUtils.LF);
		msg.append("Variable [Var1] has invalid type [Contract555]").append(StringUtils.LF);
		msg.append("Could not parse expression [Var1.Days]: Unexpected token [Var1.Days]").append(StringUtils.LF);
		msg.append("   Caused by: Unexpected token [Var1.Days]").append(StringUtils.LF);
		msg.append("Could not parse expression [Var1.Days - Var2]: Unexpected token [Var1.Days]")
				.append(StringUtils.LF);
		msg.append("   Caused by: Unexpected token [Var1.Days]").append(StringUtils.LF);
		exceptionRule.expectMessage(msg.toString());

		compileScript("TestScript33", "single-invalid-entity-script.txt");
	}

	@Test
	public void scriptWithActionCanBeCompiled() throws Exception {
		final String scriptClassName = "scriptWithActionCanBeCompiled.TestScript77";
		ScriptCompilationUnit scriptUnit = compileScript(scriptClassName, "single-action-with-params-script.txt");
		assertTrue(scriptUnit.getInputParameters().isEmpty());
		assertTrue(scriptUnit.getGeneratedSource().length() > 0);
		assertTrue(scriptUnit.getByteCode().length > 0);
	}

	@Test
	public void scriptWithInputVarsCanBeCompiled() throws Exception {
		final String scriptClassName = "scriptWithInputVarsCanBeCompiled.TestScript888";
		ScriptCompilationUnit scriptUnit = compileScript(scriptClassName, "input-vars-script.txt");
		Map<String, String> params = scriptUnit.getInputParameters();

		assertEquals(3, params.size());

		String[] paramNames = params.keySet().toArray(new String[] {});
		assertEquals("Var1", paramNames[0]);
		assertEquals("Var2", paramNames[1]);
		assertEquals("Var3", paramNames[2]);
		assertEquals("Number", params.get("Var1"));
		assertEquals("Decimal", params.get("Var2"));
		assertEquals("Number", params.get("Var3"));
	}

	@Test
	public void scriptLocalVarValuesCanBeResolved() throws Exception {
		final String scriptClassName = "scriptLocalVarValuesCanBeResolved.TestScript999";
		ScriptCompilationUnit scriptUnit = compileScript(scriptClassName,
				"local-vars-with-values-to-resolve-script.txt");

		assertTrue(scriptUnit.getInputParameters().isEmpty());
		assertTrue(scriptUnit.getGeneratedSource().length() > 0);
		assertTrue(scriptUnit.getByteCode().length > 0);
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsLocalVarsThatCanNotBeResolved() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		StringBuilder msg = new StringBuilder();
		msg.append("Script [TestScriptWithUnresolvedValues] contains errors. Compilation failed")
				.append(StringUtils.LF);
		msg.append("[1.2] is not a value of type Number").append(StringUtils.LF);
		msg.append("[abc] is not a value of type Decimal").append(StringUtils.LF);
		msg.append("Could not parse expression [abc]: Unexpected token [abc]").append(StringUtils.LF);
		msg.append("   Caused by: Unexpected token [abc]").append(StringUtils.LF);

		exceptionRule.expectMessage(msg.toString());

		compileScript("TestScriptWithUnresolvedValues", "local-vars-with-unresolved-values-script.txt");
	}

	@Test
	public void scriptInputVarsDeclarationOrderIsPreserved() throws Exception {
		final String scriptClassName = "scriptInputVarsDeclarationOrderIsPreserved.TestScript99999";
		ScriptCompilationUnit scriptUnit = compileScript(scriptClassName, "input-vars-script-2.txt");
		Map<String, String> params = scriptUnit.getInputParameters();

		assertEquals(3, params.size());

		String[] paramNames = params.keySet().toArray(new String[] {});
		assertEquals("Var3", paramNames[0]);
		assertEquals("Var1", paramNames[1]);
		assertEquals("Var2", paramNames[2]);
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsDuplicatedInputAndLocalVars() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		StringBuilder msg = new StringBuilder();
		msg.append("Script [DuplicatedInputAndLocalVars] contains errors. Compilation failed").append(StringUtils.LF);
		msg.append("Input variable with name [Var1] has been already declared").append(StringUtils.LF);
		exceptionRule.expectMessage(msg.toString());

		compileScript("DuplicatedInputAndLocalVars", "duplicated-input-and-local-vars-script.txt");
	}

	@Test
	public void scriptInputAndLocalVarsOfEntityTypesCanBeResolved() throws Exception {
		assertScriptWithParamsCanBeCompiled("scriptInputAndLocalVarsOfEntityTypesCanBeResolved.TestScript75757",
				"input-and-local-entity-vars-script.txt", 1);
	}

	@Test
	public void scriptWithArrayCanBeCompiled() throws Exception {
		assertNoParamsScriptCanBeCompiled("scriptWithArrayCanBeCompiled.TestScript7575788",
				"array-and-predefined-types-script.txt");
	}

	@Test
	public void scriptWithArrayAndEntityCanBeCompiled() throws Exception {
		assertNoParamsScriptCanBeCompiled("scriptWithArrayAndEntityCanBeCompiled.TestScript7575788",
				"array-and-entity-script.txt");
	}

	@Test
	public void scriptWithLoopCanBeCompiled() throws Exception {
		assertNoParamsScriptCanBeCompiled("scriptWithLoop.TestScript7575788", "single-loop-script.txt");
	}

	@Test
	public void entityWithSingleFieldOfPredefinedTypeCanBeCompiled() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity01";
		byte[] byteCode = ClassCompiler.compileEntity(className, "java.lang.Integer testField");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(1, clazz.getFields().length);
		final Field field = clazz.getField("testField");
		assertSame(Integer.class, field.getType());
		assertFieldIsNotInitialized(field);
	}

	@Test
	public void entityWithSeveralFieldsOfPredefinedTypeCanBeCompiled() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity02";
		byte[] byteCode = ClassCompiler.compileEntity(className,
				"java.lang.Integer testField1; java.lang.Double testField2; java.lang.Integer testField3");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(3, clazz.getFields().length);

		final Field field1 = clazz.getField("testField1");
		final Field field2 = clazz.getField("testField2");
		final Field field3 = clazz.getField("testField3");
		assertSame(Integer.class, field1.getType());
		assertSame(Double.class, field2.getType());
		assertSame(Integer.class, field3.getType());

		assertFieldsAreNotInitialized(field1, field2, field3);
	}

	@Test
	public void entityWithSingleFieldOfGeneratedTypeCanBeCompiled() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity03";
		String fieldTypeName = "com.ilsid.bfa.generated.compilertest.GeneratedContract";

		byte[] byteCode = ClassCompiler.compileEntity(className, fieldTypeName + " contract");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(1, clazz.getFields().length);
		final Field contractField = clazz.getField("contract");
		assertEquals(fieldTypeName, contractField.getType().getName());

		assertFieldIsInitialized(contractField);
	}

	@Test
	public void entityWithTwoFieldsOfSameGeneratedTypeCanBeCompiled() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity04";
		String fieldTypeName = "com.ilsid.bfa.generated.compilertest.GeneratedContract";

		byte[] byteCode = ClassCompiler.compileEntity(className,
				fieldTypeName + " contract1;" + fieldTypeName + " contract2;");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(2, clazz.getFields().length);

		final Field contract1Field = clazz.getField("contract1");
		final Field contract2Field = clazz.getField("contract2");
		assertEquals(fieldTypeName, contract1Field.getType().getName());
		assertEquals(fieldTypeName, contract2Field.getType().getName());

		assertFieldIsInitialized(contract1Field);
		assertFieldIsInitialized(contract2Field);
	}

	@Test
	public void entityWithMultipleFieldsOfGeneratedTypesCanBeCompiled() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity05";
		String fieldTypeName1 = "com.ilsid.bfa.generated.compilertest.GeneratedContract";
		String fieldTypeName2 = "com.ilsid.bfa.generated.compilertest.AnotherGeneratedContract";

		byte[] byteCode = ClassCompiler.compileEntity(className,
				fieldTypeName1 + " contract1;" + fieldTypeName2 + " contract2;");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(2, clazz.getFields().length);

		final Field contract1Field = clazz.getField("contract1");
		final Field contract2Field = clazz.getField("contract2");
		assertEquals(fieldTypeName1, contract1Field.getType().getName());
		assertEquals(fieldTypeName2, contract2Field.getType().getName());

		assertFieldIsInitialized(contract1Field);
		assertFieldIsInitialized(contract2Field);
	}

	@Test
	public void entityWithFieldsOfPredefinedAndGeneratedTypesCanBeCompiled() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity06";
		String fieldTypeName1 = "com.ilsid.bfa.generated.compilertest.GeneratedContract";
		String fieldTypeName2 = "java.lang.Integer";
		String fieldTypeName3 = "java.lang.String";

		byte[] byteCode = ClassCompiler.compileEntity(className,
				fieldTypeName1 + " contract;" + fieldTypeName2 + " days;" + fieldTypeName3 + " id;");
		Class<?> clazz = loadFromBytecode(className, byteCode);

		assertEquals(3, clazz.getFields().length);

		final Field contractField = clazz.getField("contract");
		final Field intField = clazz.getField("days");
		final Field strField = clazz.getField("id");
		assertEquals(fieldTypeName1, contractField.getType().getName());
		assertEquals(fieldTypeName2, intField.getType().getName());
		assertEquals(fieldTypeName3, strField.getType().getName());

		assertFieldIsInitialized(contractField);
		assertFieldIsNotInitialized(intField);
		assertFieldIsNotInitialized(strField);
	}

	@Test
	public void entityWithInvalidBodyCanNotBeCompiled() throws Exception {
		exceptionRule.expect(ClassCompilationException.class);
		exceptionRule.expectMessage(
				"Compilation of Entity [com.ilsid.bfa.test.generated.entity.Entity07] failed. Expression [java.lang.Integer field2 field3] is invalid");

		String className = "com.ilsid.bfa.test.generated.entity.Entity07";
		ClassCompiler.compileEntity(className, "java.lang.Double field1; java.lang.Integer field2 field3;");
	}

	@Test
	public void entityWithFieldOfInvalidTypeCanNotBeCompiled() throws Exception {
		String className = "com.ilsid.bfa.test.generated.entity.Entity08";

		exceptionRule.expect(ClassCompilationException.class);
		exceptionRule.expectMessage(String.format("Compilation of Entity [%s] failed", className));

		ClassCompiler.compileEntity(className, "java.lang.Double field1; com.some.NonExistingType field2;");
	}

	private void compileScript(String className) throws Exception {
		ClassCompiler.compileScript(className, IOHelper.loadScript("declarations-only-script.txt"));
	}

	private ScriptCompilationUnit compileScript(String className, String fileName) throws Exception {
		return ClassCompiler.compileScript(className, IOHelper.loadScript(fileName));
	}

	private Expectations getScriptExpectations() throws Exception {
		return new Expectations() {
			{
				oneOf(mockContext).addLocalVar("Var1", "java.lang.Integer");
				oneOf(mockContext).addLocalVar("Var2", "java.lang.Double");
			}
		};
	}

	private void assertNoParamsScriptCanBeCompiled(String className, String scriptName) throws Exception {
		assertScriptWithParamsCanBeCompiled(className, scriptName, 0);
	}

	private void assertScriptWithParamsCanBeCompiled(String className, String scriptName, int paramsCount)
			throws Exception {
		ScriptCompilationUnit scriptUnit = compileScript(className, scriptName);
		assertEquals(paramsCount, scriptUnit.getInputParameters().size());
		loadFromBytecode(className, scriptUnit.getByteCode()).newInstance();
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

	private void assertFieldIsInitialized(Field field) throws Exception {
		Object classInst = field.getDeclaringClass().newInstance();
		assertNotNull(field.get(classInst));
	}

	private void assertFieldIsNotInitialized(Field field) throws Exception {
		Object classInst = field.getDeclaringClass().newInstance();
		assertNull(field.get(classInst));
	}

	private void assertFieldsAreNotInitialized(Field... fields) throws Exception {
		for (Field field : fields) {
			assertFieldIsNotInitialized(field);
		}
	}

}
