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
import com.ilsid.bfa.generated.script.DummyScript;
import com.ilsid.bfa.generated.script.DummyScript$$DummyExpr;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.runtime.GlobalContext;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

public class DynamicCodeFactoryUnitTest extends BaseUnitTestCase {

	private static final String SCRIPT_CLASS_NAME_PREFIX = "com.ilsid.bfa.generated.script.";

	private static final String DUMMY_SCRIPT_NAME = "DummyScript";

	private static final GlobalContext FAKE_RUNTIME_CONTEXT = GlobalContext.getInstance();

	private static final String TEST_SCRIPT_NAME = "Test";

	private static final String SCRIPTS_DIR = "src/test/resources/dynamicCode/";

	private ScriptContext mockContext;

	private ScriptExpressionParser parser = new ScriptExpressionParser(new ScriptContext());

	@Before
	public void setUp() throws Exception {
		mockContext = mock(ScriptContext.class);
		setInaccessibleField(GlobalContext.getInstance(), "codeRepository", null);
	}

	@Test
	public void integerArithmeticExpressionCanBeInvoked() throws Exception {
		assertEquals(Integer.valueOf(1), invokeExpression("2 - 1"));
	}

	@Test
	public void doubleArithmeticExpressionCanBeInvoked() throws Exception {
		assertEquals(Double.valueOf(1.0), invokeExpression("2.0 - 1.0"));
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
		assertEquals(SCRIPT_CLASS_NAME_PREFIX + "SomeScript$$2_Mns_1",
				getInvocation("Some Script", "2 - 1").getClass().getName());
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
		verifyScript("01", "declarations-only-script.txt", new Expectations() {
			{
				oneOf(mockContext).addLocalVar("Var1", "Integer");
				oneOf(mockContext).addLocalVar("Var2", "Double");
			}
		});
	}

	@Test
	public void scriptWithSetLocalVarsCanBeInvoked() throws Exception {
		final String scriptName = "02";

		verifyScript(scriptName, "set-local-vars-script.txt", new Expectations() {
			{
				oneOf(mockContext).addLocalVar("Var1", "Integer");
				oneOf(mockContext).addLocalVar("Var2", "Double");
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
		DynamicCodeInvocation expr = DynamicCodeFactory.getInvocation(DUMMY_SCRIPT_NAME, "DummyExpr", null);
		assertSame(DummyScript$$DummyExpr.class, expr.getClass());
		assertEquals(TestConstants.DUMMY_EXPRESSION_RESULT, expr.invoke());
	}

	@Test
	public void expressionClassLoadingFailsIfRespositoryDefinedAndNoClassExists() throws Exception {
		exceptionRule.expect(DynamicCodeException.class);
		exceptionRule.expectMessage(
				"Class [" + SCRIPT_CLASS_NAME_PREFIX + "NonExistentScript$$DummyExpr] does not exist in repository");

		defineRepository();
		DynamicCodeFactory.getInvocation("NonExistentScript", "DummyExpr", null);
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
		exceptionRule.expectMessage("Class [" + SCRIPT_CLASS_NAME_PREFIX + "NonExistent] does not exist in repository");

		defineRepository();
		DynamicCodeFactory.getScript("NonExistent", null);
	}

	private void defineRepository() throws Exception {
		CodeRepository repository = new CodeRepository() {

			public byte[] load(String className) throws PersistenceException {
				if (!className.startsWith(SCRIPT_CLASS_NAME_PREFIX + DUMMY_SCRIPT_NAME)) {
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

			public void delete(String className) throws PersistenceException {
			}

			public void save(String className, byte[] byteCode, String sourceCode) throws PersistenceException {
			}

			public void update(String className, byte[] byteCode, String sourceCode) throws PersistenceException {
			}
		};

		setInaccessibleField(GlobalContext.getInstance(), "codeRepository", repository);
	}

	private void verifyScript(String scriptName, String scriptFile, Expectations expectations) throws Exception {
		Script script = createScript(scriptName, scriptFile);
		assertEquals(SCRIPT_CLASS_NAME_PREFIX + scriptName, script.getClass().getName());

		checking(expectations);

		script.setRuntimeContext(FAKE_RUNTIME_CONTEXT);
		setInaccessibleParentField(script, "scriptContext", mockContext);
		script.execute();

		assertIsSatisfed();
	}

	private DynamicCodeInvocation getInvocation(String scriptName, String origExpression) throws DynamicCodeException {
		return DynamicCodeFactory.getInvocation(scriptName, origExpression, parser);
	}

	private DynamicCodeInvocation getInvocationWithStubImplementation(String origExpression)
			throws DynamicCodeException {
		return DynamicCodeFactory.getInvocation(TEST_SCRIPT_NAME, origExpression, parser);
	}

	private Script getScriptWithStubImplementation() throws Exception {
		return createScript("Stub Script", "empty-script.txt");
	}

	private Object invokeExpression(String origExpression) throws DynamicCodeException {
		DynamicCodeInvocation invocation = DynamicCodeFactory.getInvocation(TEST_SCRIPT_NAME, origExpression, parser);
		Object result = invocation.invoke();
		return result;
	}

	private Script createScript(String scriptName, String fileName) throws Exception {
		Script script;
		try (InputStream body = loadScript(fileName);) {
			script = DynamicCodeFactory.getScript(scriptName, body);
		}
		script.setRuntimeContext(GlobalContext.getInstance());

		return script;
	}

	private InputStream loadScript(String fileName) throws Exception {
		return new FileInputStream(new File(SCRIPTS_DIR + fileName));
	}

}
