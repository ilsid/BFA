package com.ilsid.bfa.script;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.generated.entity.test.SomeEntity;
import com.ilsid.bfa.script.TypeValueResolver.DoubleResolver;
import com.ilsid.bfa.script.TypeValueResolver.EntityResolver;
import com.ilsid.bfa.script.TypeValueResolver.IntegerResolver;
import com.ilsid.bfa.script.TypeValueResolver.PredefinedTypeResolver;
import com.ilsid.bfa.script.TypeValueResolver.StringResolver;;

public class TypeValueResolverUnitTest extends BaseUnitTestCase {

	private PredefinedTypeResolver integerResolver = new IntegerResolver("Number");

	private PredefinedTypeResolver doubleResolver = new DoubleResolver("Decimal");

	private PredefinedTypeResolver stringResolver = new StringResolver("String");

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
	public void entityResolverIsUsedForNonPredefinedTypes() {
		assertEquals(EntityResolver.class, TypeValueResolver.getResolver("Non-Predefined Type").getClass());
	}

}
