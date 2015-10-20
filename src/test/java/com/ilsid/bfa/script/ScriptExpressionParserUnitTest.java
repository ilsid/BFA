package com.ilsid.bfa.script;

import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.script.ScriptExpressionParser;
import com.ilsid.bfa.script.Variable;
import com.ilsid.bfa.test.types.Contract;
import com.ilsid.bfa.test.types.Subscriber;

public class ScriptExpressionParserUnitTest extends BaseUnitTestCase {

	private ScriptExpressionParser parser;

	private ScriptContext context;

	@Before
	public void setUp() {
		context = new ScriptContext(null);
		context.setScriptName("TestScript");
		parser = new ScriptExpressionParser(context);
	}

	@Test
	public void singleIntegerCanBeParsed() throws Exception {
		assertOutput("1", "return Integer.valueOf(1);");
	}

	@Test
	public void arithmeticsWithTwoIntegerPrimitivesCanBeParsed() throws Exception {
		assertOutput("2 - 1", "return Integer.valueOf(2 - 1);");
	}
	
	@Test
	public void oddSpacesCanBeParsed() throws Exception {
		assertOutput(" 2  -  1 ", "return Integer.valueOf(2 - 1);");
	}


	@Test
	public void arithmeticsWithThreeIntegerPrimitivesCanBeParsed() throws Exception {
		assertOutput("2 - 1 + 3", "return Integer.valueOf(2 - 1 + 3);");
	}

	@Test
	public void arithmeticsWithIntegerAndDoublePrimitivesIsNotAllowed() {
		assertException("2 - 1.0",
				"Could not parse expression [2 - 1.0]: Integer value or variable is expected after operand [-], but was [1.0]");
	}

	@Test
	public void arithmeticsWithIntegerAndNonNumericIsNotAllowed() {
		assertException("2 - a",
				"Could not parse expression [2 - a]: Integer value or variable is expected after operand [-], but was [a]");
	}

	@Test
	public void consequentIntegersWoOperandsAreNotAllowed() {
		assertException("2 1",
				"Could not parse expression [2 1]: One of the operands [+-/*] is expected after [2], but was [1]");
	}

	@Test
	public void singleDoubleCanBeParsed() throws Exception {
		assertOutput("1.0", "return Double.valueOf(1.0);");
	}

	@Test
	public void arithmeticsWithTwoDoublePrimitivesCanBeParsed() throws Exception {
		assertOutput("2.0 - 1.0", "return Double.valueOf(2.0 - 1.0);");
	}

	@Test
	public void arithmeticsWithThreeDoublePrimitivesCanBeParsed() throws Exception {
		assertOutput("2.0 - 1.0 + 3.0", "return Double.valueOf(2.0 - 1.0 + 3.0);");
	}

	@Test
	public void arithmeticsWithDoubleAndIntegerPrimitivesIsNotAllowed() {
		assertException("2.0 - 1",
				"Could not parse expression [2.0 - 1]: Decimal value or variable is expected after operand [-], but was [1]");
	}

	@Test
	public void arithmeticsWithDoubleAndNonNumericIsNotAllowed() {
		assertException("2.0 - a",
				"Could not parse expression [2.0 - a]: Decimal value or variable is expected after operand [-], but was [a]");
	}

	@Test
	public void consequentDoublesWoOperandsAreNotAllowed() {
		assertException("2.0 1.0",
				"Could not parse expression [2.0 1.0]: One of the operands [+-/*] is expected after [2.0], but was [1.0]");
	}

	@Test
	public void singleOperandIsNotAllowed() {
		assertException("+", "Could not parse expression [+]: Unexpected token [+]");
	}

	@Test
	public void lastIntegerPrimitiveOperandIsNotAllowed() {
		assertException("1 +", "Could not parse expression [1 +]: Unexpected operand [+] at the end");
	}

	@Test
	public void lastDoublePrimitiveOperandIsNotAllowed() {
		assertException("1.0 +", "Could not parse expression [1.0 +]: Unexpected operand [+] at the end");
	}
	
	@Test
	public void singleIntegerVariableCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "Integer", 3));
		assertOutput("Var1", 
				"return Integer.valueOf(((Integer)scriptContext.getLocalVar(\"Var1\").getValue()).intValue());");
	}

	@Test
	public void arithmeticsWithTwoIntegerVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "Integer", 3), new Variable("Var2", "Integer", 1));
		assertOutput("Var1 - Var2", 
				"return Integer.valueOf(((Integer)scriptContext.getLocalVar(\"Var1\").getValue()).intValue()"
						+ " - ((Integer)scriptContext.getLocalVar(\"Var2\").getValue()).intValue());");

	}

	@Test
	public void arithmeticsWithThreeIntegerVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "Integer", 3), new Variable("Var2", "Integer", 1),
				new Variable("Var3", "Integer", 2));
		// System.out.println(invoke(input, output));
		assertOutput("Var1 - Var2 + Var3", 
				"return Integer.valueOf(((Integer)scriptContext.getLocalVar(\"Var1\").getValue()).intValue()"
				+ " - ((Integer)scriptContext.getLocalVar(\"Var2\").getValue()).intValue() + ((Integer)scriptContext.getLocalVar(\"Var3\").getValue()).intValue());");
	}
	
	@Test
	public void arithmeticsWithIntegerVariableAndIntegerPrimitiveCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "Integer", 3));
		assertOutput("Var1 - 1", 
				"return Integer.valueOf(((Integer)scriptContext.getLocalVar(\"Var1\").getValue()).intValue() - 1);");
	}
	
	@Test
	public void lastIntegerVariableOperandIsNotAllowed() {
		createContext(new Variable("Var1", "Integer", 3));
		assertException("Var1 +", "Could not parse expression [Var1 +]: Unexpected operand [+] at the end");
	}
	
	@Test
	public void arithmeticsWithIntegerVariableAndDoublePrimitivesIsNotAllowed() {
		createContext(new Variable("Var1", "Integer", 3));
		assertException("Var1 - 1.0",
				"Could not parse expression [Var1 - 1.0]: Integer value or variable is expected after operand [-], but was [1.0]");
	}

	@Test
	public void arithmeticsWithIntegerVariableAndNonNumericIsNotAllowed() {
		createContext(new Variable("Var1", "Integer", 3));
		assertException("Var1 - a",
				"Could not parse expression [Var1 - a]: Integer value or variable is expected after operand [-], but was [a]");
	}

	@Test
	public void consequentIntegerVariablesWoOperandsAreNotAllowed() {
		createContext(new Variable("Var1", "Integer", 3), new Variable("Var2", "Integer", 1));
		assertException("Var1 Var2",
				"Could not parse expression [Var1 Var2]: One of the operands [+-/*] is expected after [Var1], but was [Var2]");
	}
	
	@Test
	public void singleDoubleVariableCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "Double", 3.0));
		assertOutput("Var1", 
				"return Double.valueOf(((Double)scriptContext.getLocalVar(\"Var1\").getValue()).doubleValue());");
	}

	@Test
	public void arithmeticsWithTwoDoubleVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "Double", 3.0), new Variable("Var2", "Double", 1.0));
		assertOutput("Var1 - Var2", 
				"return Double.valueOf(((Double)scriptContext.getLocalVar(\"Var1\").getValue()).doubleValue()"
						+ " - ((Double)scriptContext.getLocalVar(\"Var2\").getValue()).doubleValue());");

	}

	@Test
	public void arithmeticsWithThreeDoubleVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "Double", 3.0), new Variable("Var2", "Double", 1.0),
				new Variable("Var3", "Double", 2.0));
		// System.out.println(invoke(input, output));
		assertOutput("Var1 - Var2 + Var3", 
				"return Double.valueOf(((Double)scriptContext.getLocalVar(\"Var1\").getValue()).doubleValue()"
				+ " - ((Double)scriptContext.getLocalVar(\"Var2\").getValue()).doubleValue() + ((Double)scriptContext.getLocalVar(\"Var3\").getValue()).doubleValue());");
	}
	
	@Test
	public void arithmeticsWithDoubleVariableAndDoublePrimitiveCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "Double", 3));
		assertOutput("Var1 - 1.0", 
				"return Double.valueOf(((Double)scriptContext.getLocalVar(\"Var1\").getValue()).doubleValue() - 1.0);");
	}
	
	@Test
	public void lastDoubleVariableOperandIsNotAllowed() {
		createContext(new Variable("Var1", "Double", 3.0));
		assertException("Var1 +", "Could not parse expression [Var1 +]: Unexpected operand [+] at the end");
	}
	
	@Test
	public void arithmeticsWithDoubleVariableAndIntegerPrimitivesIsNotAllowed() {
		createContext(new Variable("Var1", "Double", 3.0));
		assertException("Var1 - 1",
				"Could not parse expression [Var1 - 1]: Decimal value or variable is expected after operand [-], but was [1]");
	}

	@Test
	public void arithmeticsWithDoubleVariableAndNonNumericIsNotAllowed() {
		createContext(new Variable("Var1", "Double", 3));
		assertException("Var1 - a",
				"Could not parse expression [Var1 - a]: Decimal value or variable is expected after operand [-], but was [a]");
	}

	@Test
	public void consequentDoubleVariablesWoOperandsAreNotAllowed() {
		createContext(new Variable("Var1", "Double", 3.0), new Variable("Var2", "Double", 1.0));
		assertException("Var1 Var2",
				"Could not parse expression [Var1 Var2]: One of the operands [+-/*] is expected after [Var1], but was [Var2]");
	}
	
	@Test
	public void singleIntegerFieldCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));
		assertOutput("Contract.Days", 
				"return Integer.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).Days.intValue());");
	}
	
	@Test
	public void arithmeticsWithTwoIntegerFieldsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()), 
					new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));
		
		assertOutput("Contract.Days - Subscriber.PrepaidDays", 
				"return Integer.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).Days.intValue()"
						+ " - ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getLocalVar(\"Subscriber\").getValue()).PrepaidDays.intValue());");
	}

	@Test
	public void arithmeticsWithThreeIntegerFieldsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()), 
					new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));
	
		assertOutput("Contract.Days + Contract.ProlongDays - Subscriber.PrepaidDays", 
				"return Integer.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).Days.intValue()"
						+ " + ((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).ProlongDays.intValue()" 
						+ " - ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getLocalVar(\"Subscriber\").getValue()).PrepaidDays.intValue());");
	}
	
	@Test
	public void arithmeticsWithIntegerFieldAndIntegerPrimitiveCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
					new Variable("PrepaidDays", "Integer", 30));
		
		assertOutput("Contract.Days - PrepaidDays", 
				"return Integer.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).Days.intValue()" 
						+ " - ((Integer)scriptContext.getLocalVar(\"PrepaidDays\").getValue()).intValue());");
						
	}
	
	@Test
	public void arithmeticsWithIntegerFieldAndIntegerVariableCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));
		
		assertOutput("Contract.Days - 1", 
				"return Integer.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).Days.intValue() - 1);");
	}
	
	@Test
	public void nonExsistenIntegerFieldIsNotAllowed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));
		
		assertException("Contract.NonExistentField - 1", 
				"Could not parse expression [Contract.NonExistentField - 1]: Unexpected token [Contract.NonExistentField]");
	}

	
	@Test
	public void singleDoubleFieldCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));
		assertOutput("Contract.MonthlyFee", 
				"return Double.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).MonthlyFee.doubleValue());");
	}
	
	@Test
	public void arithmeticsWithTwoDoubleFieldsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()), 
					new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));
		
		assertOutput("Contract.MonthlyFee - Subscriber.PrepaidAmount", 
				"return Double.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).MonthlyFee.doubleValue()"
						+ " - ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getLocalVar(\"Subscriber\").getValue()).PrepaidAmount.doubleValue());");
	}

	@Test
	public void arithmeticsWithThreeDoubleFieldsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()), 
					new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));
	
		assertOutput("Contract.MonthlyFee - Subscriber.PrepaidAmount + Subscriber.PrepaidReserved", 
				"return Double.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).MonthlyFee.doubleValue()"
						+ " - ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getLocalVar(\"Subscriber\").getValue()).PrepaidAmount.doubleValue()" 
						+ " + ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getLocalVar(\"Subscriber\").getValue()).PrepaidReserved.doubleValue());");
	}
	
	@Test
	public void arithmeticsWithDoubleFieldAndDoublePrimitiveCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
					new Variable("PrepaidAmount", "Double", 30.0));
		
		assertOutput("Contract.MonthlyFee - PrepaidAmount", 
				"return Double.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).MonthlyFee.doubleValue()" 
						+ " - ((Double)scriptContext.getLocalVar(\"PrepaidAmount\").getValue()).doubleValue());");
	}
	
	@Test
	public void arithmeticsWithDoubleFieldAndDoubleVariableCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));
		
		assertOutput("Contract.MonthlyFee - 1.0", 
				"return Double.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getLocalVar(\"Contract\").getValue()).MonthlyFee.doubleValue() - 1.0);");
	}
	
	@Test
	public void nonExsistenDoubleFieldIsNotAllowed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));
		
		assertException("Contract.NonExistentField - 1.0", 
				"Could not parse expression [Contract.NonExistentField - 1.0]: Unexpected token [Contract.NonExistentField]");
	}

	
	private void createContext(final Variable... vars) {
		context = new ScriptContext(null) {
			@Override
			public Variable getLocalVar(String name) {
				for (Variable var : vars) {
					if (name.equals(var.getName())) {
						return var;
					}
				}
				return null;
			}
		};

		parser = new ScriptExpressionParser(context);
	}

	private void assertOutput(String input, String output) throws Exception {
		assertEquals(output, parser.parse(input));
	}

	private void assertException(String input, String expectedMsg) {
		try {
			parser.parse(input);
			fail(DynamicCodeException.class.getName() + " is expected");
		} catch (DynamicCodeException e) {
			assertEquals(expectedMsg, e.getMessage());
		}
	}

}
