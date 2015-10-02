package com.ilsid.bfa.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.jmock.Expectations;
import org.jmock.syntax.ReceiverClause;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.script.Action;
import com.ilsid.bfa.script.DynamicCodeException;
import com.ilsid.bfa.script.DynamicCodeFactory;
import com.ilsid.bfa.script.DynamicCodeInvocation;
import com.ilsid.bfa.script.GlobalContext;
import com.ilsid.bfa.script.Script;
import com.ilsid.bfa.script.ScriptException;
import com.ilsid.bfa.script.TypeResolver;

public class DynamicCodeFactoryUnitTest extends BaseUnitTestCase {

	private static final String EXPRESSION_CLASS_NAME_PREFIX = DynamicCodeFactory.GENERATED_PACKAGE + "TestScript$";

	private static final String SCRIPTS_DIR = "src/test/resources/dynamicCode/";

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
	public void generatedClassNameIsPrependedWithScriptName() throws Exception {
		assertEquals(DynamicCodeFactory.GENERATED_PACKAGE + "SomeScript$2_Mns_1",
				getInvocation("Some Script", "2 - 1", "return Integer.valueOf(2 - 1);").getClass().getName());
	}

	@Test
	public void scriptWithDeclarationsOnlyCanBeInvoked() throws Exception {
		Script script = createScript("Script01", "declarations-only.txt");
		assertEquals(DynamicCodeFactory.GENERATED_PACKAGE + "Script01", script.getClass().getName());

		final ScriptContext mockContext = mock(ScriptContext.class);
		checking(new Expectations() {
			{
				exactly(2).of(mockContext).addLocalVar(with(any(String.class)), with(any(String.class)));
			}
		});

		script.setGlobalContext(new GlobalContext());
		setInaccessibleParentField(script, "scriptContext", mockContext);
		script.execute();

		assertIsSatisfed();
	}

	@Test
	public void scriptWithSetLocalVarsCanBeInvoked() throws Exception {
		final String scriptName = "Script02";
		Script script = createScript(scriptName, "set-local-vars.txt");
		assertEquals(DynamicCodeFactory.GENERATED_PACKAGE + scriptName, script.getClass().getName());

		final ScriptContext mockContext = mock(ScriptContext.class);
		checking(new Expectations() {
			{
				exactly(2).of(mockContext).addLocalVar(with(any(String.class)), with(any(String.class)));
				exactly(2).of(mockContext).getScriptName();
				will(returnValue(scriptName));
				exactly(2).of(mockContext).updateLocalVar(with(any(String.class)), with(any(Object.class)));
			}
		});

		script.setGlobalContext(new GlobalContext());
		setInaccessibleParentField(script, "scriptContext", mockContext);
		script.execute();

		assertIsSatisfed();
	}

	private String getClassNameWoPrefix(String expression) {
		String className = DynamicCodeFactory.generateClassName("TestScript", expression);
		assertTrue(className.startsWith(EXPRESSION_CLASS_NAME_PREFIX));

		return className.substring(EXPRESSION_CLASS_NAME_PREFIX.length());
	}

	private DynamicCodeInvocation getInvocation(String scriptName, String origExpression, String compileExpression)
			throws DynamicCodeException {
		return DynamicCodeFactory.getInvocation(scriptName, origExpression, "return null;");
	}

	private DynamicCodeInvocation getInvocationWithStubImplementation(String origExpression)
			throws DynamicCodeException {
		return DynamicCodeFactory.getInvocation("TestScript", origExpression, "return null;");
	}

	private Object invokeExpression(String origExpression, String compileExpression) throws DynamicCodeException {
		DynamicCodeInvocation invocation = DynamicCodeFactory.getInvocation("TestScript", origExpression,
				compileExpression);
		Object result = invocation.invoke();
		return result;
	}

	private Script createScript(String scriptName, String fileName) throws Exception {
		Script script;
		try (InputStream body = loadScript(fileName);) {
			script = DynamicCodeFactory.getScript(scriptName, body);
		}

		GlobalContext mockGlobalContext = new GlobalContext() {
			@Override
			public TypeResolver getTypeResolver() {
				return new TypeResolver() {

					public String resolveJavaTypeName(String typeName) throws ScriptException {
						return null;
					}

					public Action resolveAction(String actionName) throws ScriptException {
						return null;
					}
				};
			}
		};

		script.setGlobalContext(mockGlobalContext);

		return script;
	}

	private InputStream loadScript(String fileName) throws Exception {
		return new FileInputStream(new File(SCRIPTS_DIR + fileName));
	}

}
