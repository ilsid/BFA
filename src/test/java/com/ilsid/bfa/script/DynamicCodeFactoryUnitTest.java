package com.ilsid.bfa.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.generated.DummyScript;
import com.ilsid.bfa.generated.DummyScript$$DummyExpression;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.runtime.RuntimeContext;
import com.ilsid.bfa.script.Action;
import com.ilsid.bfa.script.DynamicCodeException;
import com.ilsid.bfa.script.DynamicCodeFactory;
import com.ilsid.bfa.script.DynamicCodeInvocation;
import com.ilsid.bfa.script.Script;
import com.ilsid.bfa.script.ScriptContext;
import com.ilsid.bfa.script.ScriptException;
import com.ilsid.bfa.script.TypeResolver;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

public class DynamicCodeFactoryUnitTest extends BaseUnitTestCase {

	private static final String DUMMY_SCRIPT_NAME = "DummyScript";

	private static final RuntimeContext FAKE_RUNTIME_CONTEXT = RuntimeContext.getInstance();

	private static final String TEST_SCRIPT_NAME = "TestScript";

	private static final String EXPRESSION_CLASS_NAME_PREFIX = DynamicCodeFactory.GENERATED_PACKAGE + TEST_SCRIPT_NAME
			+ "$$";

	private static final String SCRIPTS_DIR = "src/test/resources/dynamicCode/";

	private ScriptContext mockContext;

	@Before
	public void setUp() throws Exception {
		mockContext = mock(ScriptContext.class);
		setInaccessibleField(RuntimeContext.getInstance(), "codeRepository", null);
	}

	@Test
	public void generatedClassNameDoesNotContainSpaces() {
		assertEquals("AB", getClassNameWoPrefix("A B"));
		assertEquals("AB", getClassNameWoPrefix(" AB "));
		assertEquals("ABC", getClassNameWoPrefix(" A B C "));
	}

	@Test
	public void generatedClassNameContainsReaplacedDots() {
		assertEquals("A%dtB", getClassNameWoPrefix("A.B"));
		assertEquals("%dtAB%dt", getClassNameWoPrefix(".AB."));
		assertEquals("%dtA%dtB%dtC%dt", getClassNameWoPrefix(".A.B.C."));
	}

	@Test
	public void generatedClassNameContainsReplacedMinusSign() {
		assertEquals("A_Mns_B", getClassNameWoPrefix("A-B"));
		assertEquals("A_Mns_B", getClassNameWoPrefix("A - B"));
	}

	@Test
	public void generatedClassNameContainsReplacedPlusSign() {
		assertEquals("A_Pls_B", getClassNameWoPrefix("A+B"));
		assertEquals("A_Pls_B", getClassNameWoPrefix("A + B"));
	}

	@Test
	public void generatedClassNameContainsReplacedMultiplySign() {
		assertEquals("A_Mlt_B", getClassNameWoPrefix("A*B"));
		assertEquals("A_Mlt_B", getClassNameWoPrefix("A * B"));
	}

	@Test
	public void generatedClassNameContainsReplacedDivisionSign() {
		assertEquals("A_Div_B", getClassNameWoPrefix("A/B"));
		assertEquals("A_Div_B", getClassNameWoPrefix("A / B"));
	}

	@Test
	public void integerArithmeticExpressionCanBeInvoked() throws Exception {
		assertEquals(Integer.valueOf(1), invokeExpression("2 - 1", "return Integer.valueOf(2 - 1);"));
	}

	@Test
	public void doubleArithmeticExpressionCanBeInvoked() throws Exception {
		assertEquals(Double.valueOf(1.0), invokeExpression("2.0 - 1.0", "return Double.valueOf(2.0 - 1.0);"));
	}

	@Test
	public void sameClassIsReturnedForSameExpressionOnConsequentCalls() throws Exception {
		String expr = "5 - 1";
		DynamicCodeInvocation invc1 = getInvocationWithStubImplementation(expr);
		DynamicCodeInvocation invc2 = getInvocationWithStubImplementation(expr);
		DynamicCodeInvocation invc3 = getInvocationWithStubImplementation(expr);

		assertSame(invc1.getClass(), invc2.getClass());
		assertSame(invc2.getClass(), invc3.getClass());
	}

	@Test
	public void expressionClassNameIsPrependedWithScriptName() throws Exception {
		assertEquals(DynamicCodeFactory.GENERATED_PACKAGE + "SomeScript$$2_Mns_1",
				getInvocation("Some Script", "2 - 1", "return Integer.valueOf(2 - 1);").getClass().getName());
	}

	@Test
	public void sameClassIsReturnedForSameScriptOnConsequentCalls() throws Exception {
		Script script1 = getScriptWithStubImplementation();
		Script script2 = getScriptWithStubImplementation();
		Script script3 = getScriptWithStubImplementation();

		assertSame(script1.getClass(), script2.getClass());
		assertSame(script2.getClass(), script3.getClass());
	}

	@Test
	public void scriptWithDeclarationsOnlyCanBeInvoked() throws Exception {
		verifyScript("Script01", "declarations-only-script.txt", new Expectations() {
			{
				oneOf(mockContext).addLocalVar("Var1", "Number");
				oneOf(mockContext).addLocalVar("Var2", "Decimal");
			}
		});
	}

	@Test
	public void scriptWithSetLocalVarsCanBeInvoked() throws Exception {
		final String scriptName = "Script02";

		verifyScript(scriptName, "set-local-vars-script.txt", new Expectations() {
			{
				oneOf(mockContext).addLocalVar("Var1", "Number");
				oneOf(mockContext).addLocalVar("Var2", "Decimal");
				exactly(2).of(mockContext).getScriptName();
				will(returnValue(scriptName));
				oneOf(mockContext).updateLocalVar("Var1", 1);
				oneOf(mockContext).updateLocalVar("Var2", 2.0);
			}
		});
	}

	@Test
	public void expressionClassIsLoadedFromRepositoryIfRespositoryDefined() throws Exception {
		defineRepository();
		DynamicCodeInvocation expr = DynamicCodeFactory.getInvocation(DUMMY_SCRIPT_NAME, "DummyExpression",
				"dummy java expr");
		assertSame(DummyScript$$DummyExpression.class, expr.getClass());
		assertEquals(TestConstants.DUMMY_EXPRESSION_RESULT, expr.invoke());
	}

	@Test
	public void expressionClassLoadingFailsIfRespositoryDefinedAndNoClassExists() throws Exception {
		exceptionRule.expect(DynamicCodeException.class);
		exceptionRule.expectMessage("Class [" + DynamicCodeFactory.GENERATED_PACKAGE
				+ "NonExistentScript$$DummyExpression] does not exist in repository");

		defineRepository();
		DynamicCodeFactory.getInvocation("NonExistentScript", "DummyExpression", "dummy java expr");
	}

	@Test
	public void scriptClassIsLoadedFromRepositoryIfRespositoryDefined() throws Exception {
		defineRepository();
		Script script = DynamicCodeFactory.getScript(DUMMY_SCRIPT_NAME, null);
		assertSame(DummyScript.class, script.getClass());
		DummyScript dummyScript = (DummyScript) script;
		assertNull(dummyScript.getResult());
		script.execute();
		assertEquals(TestConstants.DUMMY_SCRIPT_RESULT, dummyScript.getResult());
	}

	@Test
	public void scriptClassLoadingFailsIfRespositoryDefinedAndNoClassExists() throws Exception {
		exceptionRule.expect(DynamicCodeException.class);
		exceptionRule.expectMessage(
				"Class [" + DynamicCodeFactory.GENERATED_PACKAGE + "NonExistentScript] does not exist in repository");

		defineRepository();
		DynamicCodeFactory.getScript("NonExistentScript", null);
	}

	private void defineRepository() throws Exception {
		CodeRepository repository = new CodeRepository() {

			public byte[] load(String className) throws PersistenceException {
				if (!className.startsWith(DynamicCodeFactory.GENERATED_PACKAGE + DUMMY_SCRIPT_NAME)) {
					return new byte[0];
				}

				byte[] result = null;
				try {
					result = ClassPool.getDefault().get(className).toBytecode();
				} catch (IOException | CannotCompileException | NotFoundException e) {
					fail("Unexepected exception: " + e);
				}
				return result;
			}
		};

		setInaccessibleField(RuntimeContext.getInstance(), "codeRepository", repository);
	}

	private void verifyScript(String scriptName, String scriptFile, Expectations expectations) throws Exception {
		Script script = createScript(scriptName, scriptFile);
		assertEquals(DynamicCodeFactory.GENERATED_PACKAGE + scriptName, script.getClass().getName());

		checking(expectations);

		script.setRuntimeContext(FAKE_RUNTIME_CONTEXT);
		setInaccessibleParentField(script, "scriptContext", mockContext);
		script.execute();

		assertIsSatisfed();
	}

	private String getClassNameWoPrefix(String expression) {
		String className = DynamicCodeFactory.generateClassName(TEST_SCRIPT_NAME, expression);
		assertTrue(className.startsWith(EXPRESSION_CLASS_NAME_PREFIX));

		return className.substring(EXPRESSION_CLASS_NAME_PREFIX.length());
	}

	private DynamicCodeInvocation getInvocation(String scriptName, String origExpression, String compileExpression)
			throws DynamicCodeException {
		return DynamicCodeFactory.getInvocation(scriptName, origExpression, "return null;");
	}

	private DynamicCodeInvocation getInvocationWithStubImplementation(String origExpression)
			throws DynamicCodeException {
		return DynamicCodeFactory.getInvocation(TEST_SCRIPT_NAME, origExpression, "return null;");
	}

	private Script getScriptWithStubImplementation() throws Exception {
		return createScript("Stub Script", "empty-script.txt");
	}

	private Object invokeExpression(String origExpression, String compileExpression) throws DynamicCodeException {
		DynamicCodeInvocation invocation = DynamicCodeFactory.getInvocation(TEST_SCRIPT_NAME, origExpression, compileExpression);
		Object result = invocation.invoke();
		return result;
	}

	private Script createScript(String scriptName, String fileName) throws Exception {
		Script script;
		try (InputStream body = loadScript(fileName);) {
			script = DynamicCodeFactory.getScript(scriptName, body);
		}

		script.setRuntimeContext(RuntimeContext.getInstance());
		script.setTypeResolver(new TypeResolver() {

			public String resolveJavaTypeName(String typeName) throws ScriptException {
				return typeName;
			}

			public Action resolveAction(String actionName) throws ScriptException {
				return null;
			}

			public Script resolveSubflow(String flowName) throws ScriptException {
				return null;
			}
		});

		return script;
	}

	private InputStream loadScript(String fileName) throws Exception {
		return new FileInputStream(new File(SCRIPTS_DIR + fileName));
	}

}
