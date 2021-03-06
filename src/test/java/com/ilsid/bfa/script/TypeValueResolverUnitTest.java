package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.generated.entity.test.SomeEntity;
import com.ilsid.bfa.script.TypeValueResolver.BooleanResolver;
import com.ilsid.bfa.script.TypeValueResolver.DoubleResolver;
import com.ilsid.bfa.script.TypeValueResolver.EntityResolver;
import com.ilsid.bfa.script.TypeValueResolver.IntegerResolver;
import com.ilsid.bfa.script.TypeValueResolver.PredefinedTypeResolver;
import com.ilsid.bfa.script.TypeValueResolver.StringResolver;
import com.ilsid.bfa.script.TypeValueResolver.ArrayResolver;
import com.ilsid.bfa.test.types.Contract;
import com.ilsid.bfa.test.types.ContractHolder;

public class TypeValueResolverUnitTest extends BaseUnitTestCase {

	private PredefinedTypeResolver integerResolver = new IntegerResolver();

	private PredefinedTypeResolver doubleResolver = new DoubleResolver();

	private PredefinedTypeResolver stringResolver = new StringResolver();

	private PredefinedTypeResolver booleanResolver = new BooleanResolver();

	private PredefinedTypeResolver arrayResolver = new ArrayResolver();

	@Test
	public void integerValueIsResolvedToIntegerForNumberType() throws Exception {
		assertEquals(1, integerResolver.resolve(1));
	}

	@Test
	public void stringValueIsResolvedToIntegerForNumberType() throws Exception {
		assertEquals(1, integerResolver.resolve("1"));
	}

	@Test
	public void improperStringValueIsNotResolvedToIntegerForNumberType() throws Exception {
		exceptionRule.expect(InvalidTypeException.class);
		exceptionRule.expectMessage("[abc] is not a value of type Number");

		integerResolver.resolve("abc");
	}

	@Test
	public void doubleValueIsNotResolvedToIntegerForNumberType() throws Exception {
		exceptionRule.expect(InvalidTypeException.class);
		exceptionRule.expectMessage("[1.0] is not a value of type Number");

		integerResolver.resolve(1.0);
	}

	@Test
	public void doubleValueIsResolvedToDoubleForDecimalType() throws Exception {
		assertEquals(1.0, doubleResolver.resolve(1.0));
	}

	@Test
	public void stringValueIsResolvedToDoubleForDecimalType() throws Exception {
		assertEquals(1.0, doubleResolver.resolve("1.0"));
	}

	@Test
	public void integerValueIsResolvedToDoubleForDecimalType() throws Exception {
		assertEquals(1.0, doubleResolver.resolve(1));
	}

	@Test
	public void integerValueAsStringIsResolvedToDoubleForDecimalType() throws Exception {
		assertEquals(1.0, doubleResolver.resolve("1"));
	}

	@Test
	public void improperStringValueIsNotResolvedToDoubleForDecimalType() throws Exception {
		exceptionRule.expect(InvalidTypeException.class);
		exceptionRule.expectMessage("[abc] is not a value of type Decimal");

		doubleResolver.resolve("abc");
	}

	@Test
	public void stringValueIsResolvedToStringForStringType() throws Exception {
		assertEquals("1", stringResolver.resolve("1"));
		assertEquals("1.0", stringResolver.resolve("1.0"));
		assertEquals("abc", stringResolver.resolve("abc"));
	}

	@Test
	public void nonStringValueIsResolvedToStringForStringType() throws Exception {
		assertEquals("1", stringResolver.resolve(1));
		assertEquals("1.0", stringResolver.resolve(1.0));

		String resolvedValue = (String) stringResolver.resolve(new Object());
		assertTrue(resolvedValue.startsWith(Object.class.getName()));
	}

	@Test
	public void booleanValueIsResolvedToBooleanForBooleanType() throws Exception {
		assertEquals(true, booleanResolver.resolve(true));
		assertEquals(false, booleanResolver.resolve(false));
	}

	@Test
	public void booleanValueAsStringIsResolvedToBooleanForBooleanType() throws Exception {
		assertEquals(true, booleanResolver.resolve("true"));
		assertEquals(false, booleanResolver.resolve("false"));
		assertEquals(true, booleanResolver.resolve("True"));
		assertEquals(false, booleanResolver.resolve("False"));
		assertEquals(true, booleanResolver.resolve("TruE"));
		assertEquals(false, booleanResolver.resolve("FaLsE"));
	}

	@Test
	public void improperBooleanValueIsNotResolvedToBooleanForBooleanType() throws Exception {
		exceptionRule.expect(InvalidTypeException.class);
		exceptionRule.expectMessage("[abc] is not a value of type Boolean");

		booleanResolver.resolve("abc");
	}

	@Test
	public void arrayValueIsResolvedToSameInstance() throws Exception {
		Object[] array = new Object[] {};
		assertSame(array, arrayResolver.resolve(array));

		array = new Object[] { null };
		assertSame(array, arrayResolver.resolve(array));

		array = new Object[] { "aaa", new Object(), 22 };
		assertSame(array, arrayResolver.resolve(array));
	}

	@Test
	public void nonArrayValueIsNotResolvedToArray() throws Exception {
		exceptionRule.expect(InvalidTypeException.class);
		exceptionRule.expectMessage("[abcde] is not a value of type Array");

		arrayResolver.resolve("abcde");
	}

	@Test
	public void enityValueIsResolvedToSameEntity() throws Exception {
		EntityResolver resolver = new EntityResolver(SomeEntity.class.getName());
		SomeEntity entity = new SomeEntity();
		assertSame(entity, resolver.resolve(entity));
	}

	@Test
	public void entityValueWithUnexpectedNameIsNotResolved() throws Exception {
		exceptionRule.expect(InvalidTypeException.class);
		exceptionRule.expectMessage("[SomeEntity] is not a value of type AnotherEntity");

		EntityResolver resolver = new EntityResolver(SomeEntity.class.getName().replace("SomeEntity", "AnotherEntity"));
		resolver.resolve(new SomeEntity());
	}

	@Test
	public void jsonStringValueIsResolvedToEntity() throws Exception {
		EntityResolver resolver = new EntityResolver(Contract.class.getName());
		Contract contract = (Contract) resolver.resolve("{\"Days\":\"33\",\"MonthlyFee\":\"77.99\",\"ID\":\"abc\"}");

		assertEquals(33, contract.Days);
		assertEquals(77.99, contract.MonthlyFee);
		assertEquals("abc", contract.ID);
	}

	@Test
	public void mapInstanceIsResolvedToEntity() throws Exception {
		Map<String, String> value = new HashMap<>();
		value.put("Days", "33");
		value.put("MonthlyFee", "77.99");
		value.put("ID", "abc");

		EntityResolver resolver = new EntityResolver(Contract.class.getName());
		Contract contract = (Contract) resolver.resolve(value);

		assertEquals(33, contract.Days);
		assertEquals(77.99, contract.MonthlyFee);
		assertEquals("abc", contract.ID);
	}

	@Test
	public void compositeMapInstanceIsResolvedToEntity() throws Exception {
		Map<String, String> nestedValue = new HashMap<>();
		nestedValue.put("Days", "33");
		nestedValue.put("MonthlyFee", "77.99");
		nestedValue.put("ID", "abc");

		Map<String, Object> value = new HashMap<>();
		value.put("ID", "11");
		value.put("Contract", nestedValue);

		EntityResolver resolver = new EntityResolver(ContractHolder.class.getName());
		ContractHolder contractHolder = (ContractHolder) resolver.resolve(value);

		assertEquals(11, contractHolder.ID);
		assertEquals(33, contractHolder.Contract.Days);
		assertEquals(77.99, contractHolder.Contract.MonthlyFee);
		assertEquals("abc", contractHolder.Contract.ID);
	}

	@Test
	public void invalidJsonStringValueIsNotResolvedToEntity() throws Exception {
		exceptionRule.expect(InvalidTypeException.class);
		exceptionRule.expectMessage("[{\"nonContractField\":\"someValue\"}] is not a value of type Contract");

		EntityResolver resolver = new EntityResolver(Contract.class.getName());
		resolver.resolve("{\"nonContractField\":\"someValue\"}");
	}

	@Test
	public void integerResolverIsUsedForPredefinedNumberType() {
		assertSame(PredefinedTypes.getResolver("java.lang.Integer"),
				TypeValueResolver.getResolver("java.lang.Integer"));
	}

	@Test
	public void doubleResolverIsUsedForPredefinedDecimalType() {
		assertSame(PredefinedTypes.getResolver("java.lang.Double"), TypeValueResolver.getResolver("java.lang.Double"));
	}

	@Test
	public void stringResolverIsUsedForPredefinedStringType() {
		assertSame(PredefinedTypes.getResolver("java.lang.String"), TypeValueResolver.getResolver("java.lang.String"));
	}

	@Test
	public void booleanResolverIsUsedForPredefinedStringBoolean() {
		assertSame(PredefinedTypes.getResolver("java.lang.Boolean"),
				TypeValueResolver.getResolver("java.lang.Boolean"));
	}

	@Test
	public void entityResolverIsUsedForNonPredefinedTypes() {
		assertEquals(EntityResolver.class, TypeValueResolver.getResolver("Non-Predefined Type").getClass());
	}

}
