package com.ilsid.bfa.common;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.ConfigurationException;

public class ConfigUtilUnitTest extends BaseUnitTestCase {

	private static final String POSITIVE_INT_PROP = "positive.int.prop";

	private static final String ZERO_INT_PROP = "zero.int.prop";

	private static final String NEGATIVE_INT_PROP = "negative.int.prop";

	private static final String NON_INT_PROP = "non.int.prop";

	private static final String NON_EXISTING_PROP = "non.existing.prop";

	private static final int DEFAULT_VALUE = 77;

	@SuppressWarnings("serial")
	private static final Map<String, String> config = new HashMap<String, String>() {
		{
			put(POSITIVE_INT_PROP, "33");
			put(ZERO_INT_PROP, "0");
			put(NEGATIVE_INT_PROP, "-33");
			put(NON_INT_PROP, "abc");
		}
	};

	@Test
	public void positiveIntegerValueCanBeExtracted() throws Exception {
		assertEquals(33, extractValue(POSITIVE_INT_PROP));
	}

	@Test
	public void defaultValueIsExtractedForNonExistingProperty() throws Exception {
		assertEquals(DEFAULT_VALUE, extractValue(NON_EXISTING_PROP));
	}

	@Test
	public void zeroIntegerValueCanNotBeExtracted() throws Exception {
		assertException(ZERO_INT_PROP);
	}

	@Test
	public void negativeIntegerValueCanNotBeExtracted() throws Exception {
		assertException(NEGATIVE_INT_PROP);
	}

	@Test
	public void nonIntegerValueCanNotBeExtracted() throws Exception {
		assertException(NON_INT_PROP);
	}

	private int extractValue(String propertyName) throws Exception {
		return ConfigUtil.getPositiveIntegerValue(propertyName, config, DEFAULT_VALUE);
	}

	private void assertException(String propertyName) throws Exception {
		exceptionRule.expect(ConfigurationException.class);
		exceptionRule.expectMessage(
				String.format("The value of the configuration property [%s] must be a positive integer", propertyName));

		extractValue(propertyName);
	}

}
