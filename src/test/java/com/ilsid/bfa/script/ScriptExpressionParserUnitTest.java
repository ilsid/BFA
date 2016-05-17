package com.ilsid.bfa.script;

import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.test.types.Contract;
import com.ilsid.bfa.test.types.Subscriber;

public class ScriptExpressionParserUnitTest extends BaseUnitTestCase {

	private ScriptExpressionParser parser;

	private ScriptContext context;

	@Before
	public void setUp() {
		context = new ScriptContext();
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
		assertException("2 1", "Could not parse expression [2 1]: Operand is expected after [2], but was [1]");
	}

	@Test
	public void spaceIsRequiredForArithmeticsWithTwoIntegers() {
		assertException("2-1", "Could not parse expression [2-1]: Unexpected token [2-1]");
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
				"Could not parse expression [2.0 1.0]: Operand is expected after [2.0], but was [1.0]");
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
		createContext(new Variable("Var1", "java.lang.Integer", 3));
		assertOutput("Var1",
				"return Integer.valueOf(((Integer)scriptContext.getVar(\"Var1\").getValue()).intValue());");
	}

	@Test
	public void arithmeticsWithTwoIntegerVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Integer", 3), new Variable("Var2", "java.lang.Integer", 1));
		assertOutput("Var1 - Var2",
				"return Integer.valueOf(((Integer)scriptContext.getVar(\"Var1\").getValue()).intValue()"
						+ " - ((Integer)scriptContext.getVar(\"Var2\").getValue()).intValue());");

	}

	@Test
	public void arithmeticsWithThreeIntegerVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Integer", 3), new Variable("Var2", "java.lang.Integer", 1),
				new Variable("Var3", "java.lang.Integer", 2));
		assertOutput("Var1 - Var2 + Var3",
				"return Integer.valueOf(((Integer)scriptContext.getVar(\"Var1\").getValue()).intValue()"
						+ " - ((Integer)scriptContext.getVar(\"Var2\").getValue()).intValue()"
						+ " + ((Integer)scriptContext.getVar(\"Var3\").getValue()).intValue());");
	}

	@Test
	public void arithmeticsWithIntegerVariableAndIntegerPrimitiveCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Integer", 3));
		assertOutput("Var1 - 1",
				"return Integer.valueOf(((Integer)scriptContext.getVar(\"Var1\").getValue()).intValue() - 1);");
	}

	@Test
	public void lastIntegerVariableOperandIsNotAllowed() {
		createContext(new Variable("Var1", "java.lang.Integer", 3));
		assertException("Var1 +", "Could not parse expression [Var1 +]: Unexpected operand [+] at the end");
	}

	@Test
	public void arithmeticsWithIntegerVariableAndDoublePrimitiveIsNotAllowed() {
		createContext(new Variable("Var1", "java.lang.Integer", 3));
		assertException("Var1 - 1.0",
				"Could not parse expression [Var1 - 1.0]: Integer value or variable is expected after operand [-], but was [1.0]");
	}

	@Test
	public void arithmeticsWithIntegerVariableAndNonNumericIsNotAllowed() {
		createContext(new Variable("Var1", "java.lang.Integer", 3));
		assertException("Var1 - a",
				"Could not parse expression [Var1 - a]: Integer value or variable is expected after operand [-], but was [a]");
	}

	@Test
	public void consequentIntegerVariablesWoOperandsAreNotAllowed() {
		createContext(new Variable("Var1", "java.lang.Integer", 3), new Variable("Var2", "java.lang.Integer", 1));
		assertException("Var1 Var2",
				"Could not parse expression [Var1 Var2]: Operand is expected after [Var1], but was [Var2]");
	}

	@Test
	public void singleDoubleVariableCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Double", 3.0));
		assertOutput("Var1",
				"return Double.valueOf(((Double)scriptContext.getVar(\"Var1\").getValue()).doubleValue());");
	}

	@Test
	public void arithmeticsWithTwoDoubleVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Double", 3.0), new Variable("Var2", "java.lang.Double", 1.0));
		assertOutput("Var1 - Var2",
				"return Double.valueOf(((Double)scriptContext.getVar(\"Var1\").getValue()).doubleValue()"
						+ " - ((Double)scriptContext.getVar(\"Var2\").getValue()).doubleValue());");

	}

	@Test
	public void arithmeticsWithThreeDoubleVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Double", 3.0), new Variable("Var2", "java.lang.Double", 1.0),
				new Variable("Var3", "java.lang.Double", 2.0));
		// System.out.println(invoke(input, output));
		assertOutput("Var1 - Var2 + Var3",
				"return Double.valueOf(((Double)scriptContext.getVar(\"Var1\").getValue()).doubleValue()"
						+ " - ((Double)scriptContext.getVar(\"Var2\").getValue()).doubleValue()"
						+ " + ((Double)scriptContext.getVar(\"Var3\").getValue()).doubleValue());");
	}

	@Test
	public void arithmeticsWithDoubleVariableAndDoublePrimitiveCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Double", 3));
		assertOutput("Var1 - 1.0",
				"return Double.valueOf(((Double)scriptContext.getVar(\"Var1\").getValue()).doubleValue() - 1.0);");
	}

	@Test
	public void lastDoubleVariableOperandIsNotAllowed() {
		createContext(new Variable("Var1", "java.lang.Double", 3.0));
		assertException("Var1 +", "Could not parse expression [Var1 +]: Unexpected operand [+] at the end");
	}

	@Test
	public void arithmeticsWithDoubleVariableAndIntegerPrimitiveIsNotAllowed() {
		createContext(new Variable("Var1", "java.lang.Double", 3.0));
		assertException("Var1 - 1",
				"Could not parse expression [Var1 - 1]: Decimal value or variable is expected after operand [-], but was [1]");
	}

	@Test
	public void arithmeticsWithDoubleVariableAndNonNumericIsNotAllowed() {
		createContext(new Variable("Var1", "java.lang.Double", 3));
		assertException("Var1 - a",
				"Could not parse expression [Var1 - a]: Decimal value or variable is expected after operand [-], but was [a]");
	}

	@Test
	public void consequentDoubleVariablesWoOperandsAreNotAllowed() {
		createContext(new Variable("Var1", "java.lang.Double", 3.0), new Variable("Var2", "java.lang.Double", 1.0));
		assertException("Var1 Var2",
				"Could not parse expression [Var1 Var2]: Operand is expected after [Var1], but was [Var2]");
	}

	@Test
	public void singleIntegerFieldCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));
		assertOutput("Contract.Days",
				"return Integer.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).Days.intValue());");
	}

	@Test
	public void arithmeticsWithTwoIntegerFieldsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));

		assertOutput("Contract.Days - Subscriber.PrepaidDays",
				"return Integer.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).Days.intValue()"
						+ " - ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getVar(\"Subscriber\").getValue()).PrepaidDays.intValue());");
	}

	@Test
	public void arithmeticsWithThreeIntegerFieldsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));

		assertOutput("Contract.Days + Contract.ProlongDays - Subscriber.PrepaidDays",
				"return Integer.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).Days.intValue()"
						+ " + ((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).ProlongDays.intValue()"
						+ " - ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getVar(\"Subscriber\").getValue()).PrepaidDays.intValue());");
	}

	@Test
	public void arithmeticsWithIntegerFieldAndIntegerVariableCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("PrepaidDays", "java.lang.Integer", 30));

		assertOutput("Contract.Days - PrepaidDays",
				"return Integer.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).Days.intValue()"
						+ " - ((Integer)scriptContext.getVar(\"PrepaidDays\").getValue()).intValue());");

	}

	@Test
	public void arithmeticsWithIntegerFieldAndIntegerPrimitiveCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));

		assertOutput("Contract.Days - 1",
				"return Integer.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).Days.intValue() - 1);");
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
				"return Double.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).MonthlyFee.doubleValue());");
	}

	@Test
	public void arithmeticsWithTwoDoubleFieldsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));

		assertOutput("Contract.MonthlyFee - Subscriber.PrepaidAmount",
				"return Double.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).MonthlyFee.doubleValue()"
						+ " - ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getVar(\"Subscriber\").getValue()).PrepaidAmount.doubleValue());");
	}

	@Test
	public void arithmeticsWithThreeDoubleFieldsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));

		assertOutput("Contract.MonthlyFee - Subscriber.PrepaidAmount + Subscriber.PrepaidReserved",
				"return Double.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).MonthlyFee.doubleValue()"
						+ " - ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getVar(\"Subscriber\").getValue()).PrepaidAmount.doubleValue()"
						+ " + ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getVar(\"Subscriber\").getValue()).PrepaidReserved.doubleValue());");
	}

	@Test
	public void arithmeticsWithDoubleFieldAndDoubleVariableCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("PrepaidAmount", "java.lang.Double", 30.0));

		assertOutput("Contract.MonthlyFee - PrepaidAmount",
				"return Double.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).MonthlyFee.doubleValue()"
						+ " - ((Double)scriptContext.getVar(\"PrepaidAmount\").getValue()).doubleValue());");
	}

	@Test
	public void arithmeticsWithDoubleFieldAndDoublePrimitiveCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));

		assertOutput("Contract.MonthlyFee - 1.0",
				"return Double.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).MonthlyFee.doubleValue() - 1.0);");
	}

	@Test
	public void nonExsistenDoubleFieldIsNotAllowed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));

		assertException("Contract.NonExistentField - 1.0",
				"Could not parse expression [Contract.NonExistentField - 1.0]: Unexpected token [Contract.NonExistentField]");
	}

	@Test
	public void singleTrueBooleanCanBeParsed() throws Exception {
		assertOutput("true", "return Boolean.valueOf(true);");
	}

	@Test
	public void singleFalseBooleanCanBeParsed() throws Exception {
		assertOutput("false", "return Boolean.valueOf(false);");
	}

	@Test
	public void logicalOperationWithTwoBooleanPrimitivesCanBeParsed() throws Exception {
		assertOutput("false && true", "return Boolean.valueOf(false && true);");
		assertOutput("false || true", "return Boolean.valueOf(false || true);");
	}

	@Test
	public void logicalOperationWithThreeBooleanPrimitivesCanBeParsed() throws Exception {
		assertOutput("false && true || true", "return Boolean.valueOf(false && true || true);");
	}

	@Test
	public void logicalOperationWithBooleanAndIntegerPrimitivesIsNotAllowed() {
		assertException("true && 1",
				"Could not parse expression [true && 1]: Boolean value or variable is expected after operand [&&], but was [1]");
	}

	@Test
	public void logicalOperationWithBooleanAndNonNumericIsNotAllowed() {
		assertException("true && abc",
				"Could not parse expression [true && abc]: Boolean value or variable is expected after operand [&&], but was [abc]");
	}

	@Test
	public void consequentBooleansWoOperandsAreNotAllowed() {
		assertException("true false",
				"Could not parse expression [true false]: Operand is expected after [true], but was [false]");
	}

	@Test
	public void lastBooleanPrimitiveOperandIsNotAllowed() {
		assertException("false ||", "Could not parse expression [false ||]: Unexpected operand [||] at the end");
	}

	@Test
	public void singleBooleanVariableCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Boolean", true));
		assertOutput("Var1",
				"return Boolean.valueOf(((Boolean)scriptContext.getVar(\"Var1\").getValue()).booleanValue());");
	}

	@Test
	public void logicalOperationWithTwoBooleanVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Boolean", true),
				new Variable("Var2", "java.lang.Boolean", false));
		assertOutput("Var1 && Var2",
				"return Boolean.valueOf(((Boolean)scriptContext.getVar(\"Var1\").getValue()).booleanValue()"
						+ " && ((Boolean)scriptContext.getVar(\"Var2\").getValue()).booleanValue());");
	}

	@Test
	public void logicalOperationWithThreeBooleanVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Boolean", true), new Variable("Var2", "java.lang.Boolean", false),
				new Variable("Var3", "java.lang.Boolean", true));
		assertOutput("Var1 && Var2 || Var3",
				"return Boolean.valueOf(((Boolean)scriptContext.getVar(\"Var1\").getValue()).booleanValue()"
						+ " && ((Boolean)scriptContext.getVar(\"Var2\").getValue()).booleanValue()"
						+ " || ((Boolean)scriptContext.getVar(\"Var3\").getValue()).booleanValue());");
	}

	@Test
	public void logicalOperationWithBooleanVariableAndBooleanPrimitiveCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.Boolean", true));
		assertOutput("Var1 && true",
				"return Boolean.valueOf(((Boolean)scriptContext.getVar(\"Var1\").getValue()).booleanValue() && true);");
	}

	@Test
	public void lastBooleanVariableOperandIsNotAllowed() {
		createContext(new Variable("Var1", "java.lang.Boolean", true));
		assertException("Var1 &&", "Could not parse expression [Var1 &&]: Unexpected operand [&&] at the end");
	}

	@Test
	public void logicalOperationWithBooleanVariableAndIntegerPrimitiveIsNotAllowed() {
		createContext(new Variable("Var1", "java.lang.Boolean", true));
		assertException("Var1 && 1",
				"Could not parse expression [Var1 && 1]: Boolean value or variable is expected after operand [&&], but was [1]");
	}

	@Test
	public void singleBooleanFieldCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));
		assertOutput("Contract.IsValid",
				"return Boolean.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).IsValid.booleanValue());");
	}

	@Test
	public void logicalOperationWithTwoBooleanFieldsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));

		assertOutput("Contract.IsValid && Subscriber.IsPrepaid",
				"return Boolean.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).IsValid.booleanValue()"
						+ " && ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getVar(\"Subscriber\").getValue()).IsPrepaid.booleanValue());");
	}

	@Test
	public void logicalOperationWithThreeBooleanFieldsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));

		assertOutput("Contract.IsValid && Contract.IsAnnual && Subscriber.IsPrepaid",
				"return Boolean.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).IsValid.booleanValue()"
						+ " && ((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).IsAnnual.booleanValue()"
						+ " && ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getVar(\"Subscriber\").getValue()).IsPrepaid.booleanValue());");
	}

	@Test
	public void logicalOperationWithBooleanFieldAndBooleanVariableCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("IsPrepaid", "java.lang.Boolean", true));

		assertOutput("Contract.IsValid && IsPrepaid",
				"return Boolean.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).IsValid.booleanValue()"
						+ " && ((Boolean)scriptContext.getVar(\"IsPrepaid\").getValue()).booleanValue());");

	}

	@Test
	public void logicalOperationWithBooleanFieldAndBooleanPrimitiveCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()));

		assertOutput("Contract.IsValid && true",
				"return Boolean.valueOf(((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).IsValid.booleanValue() && true);");
	}

	@Test
	public void singleStringLiteralCanBeParsed() throws Exception {
		assertOutput("'abc'", "return (\"abc\");");
	}

	@Test
	public void emptyStringLiteralCanBeParsed() throws Exception {
		assertOutput("''", "return (\"\");");
	}

	@Test
	public void blankStringLiteralCanBeParsed() throws Exception {
		assertOutput("' '", "return (\" \");");
		assertOutput("'   '", "return (\"   \");");
	}

	@Test
	public void stringLiteralWithLeadingBlankCanBeParsed() throws Exception {
		assertOutput("' abc'", "return (\" abc\");");
		assertOutput("'   abc'", "return (\"   abc\");");
	}

	@Test
	public void stringLiteralWithTrailingBlankCanBeParsed() throws Exception {
		assertOutput("'abc '", "return (\"abc \");");
		assertOutput("'abc   '", "return (\"abc   \");");
	}

	@Test
	public void stringLiteralWithBlankInTheMiddleCanBeParsed() throws Exception {
		assertOutput("'ab c'", "return (\"ab c\");");
		assertOutput("'ab   c'", "return (\"ab   c\");");
	}

	@Test
	public void concatenationOfTwoStringLiteralsCanBeParsed() throws Exception {
		assertOutput("'abc' + 'fgh'", "return (\"abc\" + \"fgh\");");
	}

	@Test
	public void concatenationOfThreeStringLiteralsCanBeParsed() throws Exception {
		assertOutput("'abc' + 'fgh' + '123'", "return (\"abc\" + \"fgh\" + \"123\");");
	}

	@Test
	public void spaceOnBothSidesIsRequiredForConcatenationOfTwoStringLiterals() throws Exception {
		assertException("'abc'+'fgh'", "Could not parse expression ['abc'+'fgh']: Unexpected token ['abc'+'fgh']");
		assertException("'abc'+ 'fgh'", "Could not parse expression ['abc'+ 'fgh']: Unexpected token ['abc'+]");
		assertException("'abc' +'fgh'",
				"Could not parse expression ['abc' +'fgh']: Operand is expected after ['abc'], but was [+'fgh']");
	}

	@Test
	public void singleStringLiteralWithDoubleQuotesIsNotAllowed() throws Exception {
		assertException("\"abc\"", "Could not parse expression [\"abc\"]: Unexpected token [\"abc\"]");
	}

	@Test
	public void consequentStringsWoOperandsAreNotAllowed() {
		assertException("'abc' 'fgh'",
				"Could not parse expression ['abc' 'fgh']: Operand is expected after ['abc'], but was ['fgh']");
	}

	@Test
	public void lastStringConcatenationOperandIsNotAllowed() {
		assertException("'abc' +", "Could not parse expression ['abc' +]: Unexpected operand [+] at the end");
	}

	@Test
	public void concatenationOfStringLiteralAndNonStringIsNotAllowed() {
		assertException("'abc' + 1",
				"Could not parse expression ['abc' + 1]: String value or variable is expected after operand [+], but was [1]");
	}

	@Test
	public void singleStringVariableCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.String", "abc"));
		assertOutput("Var1", "return ((String)scriptContext.getVar(\"Var1\").getValue());");
	}

	@Test
	public void concatenationOfTwoStringVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.String", "abc"), new Variable("Var2", "java.lang.String", "fgh"));
		assertOutput("Var1 + Var2", "return ((String)scriptContext.getVar(\"Var1\").getValue() "
				+ "+ (String)scriptContext.getVar(\"Var2\").getValue());");
	}

	@Test
	public void concatenationOfThreeStringVariablesCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.String", "abc"), new Variable("Var2", "java.lang.String", "fgh"),
				new Variable("Var3", "java.lang.String", "xyz"));
		assertOutput("Var1 + Var2 + Var3",
				"return ((String)scriptContext.getVar(\"Var1\").getValue() "
						+ "+ (String)scriptContext.getVar(\"Var2\").getValue() "
						+ "+ (String)scriptContext.getVar(\"Var3\").getValue());");
	}

	@Test
	public void concatenationOfStringVariableAndStringLiteralCanBeParsed() throws Exception {
		createContext(new Variable("Var1", "java.lang.String", "abc"));
		assertOutput("Var1 + 'xyz'", "return ((String)scriptContext.getVar(\"Var1\").getValue() + \"xyz\");");
	}

	@Test
	public void singleStringFieldCanBeParsed() throws Exception {
		createContext(new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));
		assertOutput("Subscriber.MSISDN",
				"return (((com.ilsid.bfa.test.types.Subscriber)scriptContext.getVar(\"Subscriber\").getValue()).MSISDN);");
	}

	@Test
	public void concatenationOfStringFieldsAndStringLiteralsCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("Subscriber", Subscriber.class.getName(), new Subscriber()));
		assertOutput("'{' + Contract.ID + ': ' + Subscriber.MSISDN + '}'",
				"return (\"{\" + ((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).ID "
						+ "+ \": \" + ((com.ilsid.bfa.test.types.Subscriber)scriptContext.getVar(\"Subscriber\").getValue()).MSISDN + \"}\");");
	}

	@Test
	public void concatenationOfStringFieldStringLiteralAndStringVariableCanBeParsed() throws Exception {
		createContext(new Variable("Contract", Contract.class.getName(), new Contract()),
				new Variable("Var1", "java.lang.String", "abc"));
		assertOutput("Contract.ID + ': ' + Var1",
				"return (((com.ilsid.bfa.test.types.Contract)scriptContext.getVar(\"Contract\").getValue()).ID "
						+ "+ \": \" + (String)scriptContext.getVar(\"Var1\").getValue());");
	}

	private void createContext(final Variable... vars) {
		context = ScriptContextUtil.createContext(vars);
		parser = new ScriptExpressionParser(context);
	}

	private void assertOutput(String input, String output) throws Exception {
		assertEquals(output, parser.parse(input));
	}

	private void assertException(String input, String expectedMsg) {
		try {
			parser.parse(input);
			fail(ParsingException.class.getName() + " is expected");
		} catch (ParsingException e) {
			assertEquals(expectedMsg, e.getMessage());
		}
	}

}
