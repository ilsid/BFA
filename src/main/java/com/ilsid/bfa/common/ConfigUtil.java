package com.ilsid.bfa.common;

import java.util.Map;

import com.ilsid.bfa.ConfigurationException;

/**
 * Provides configuration related routines.
 * 
 * @author illia.sydorovych
 *
 */
public class ConfigUtil {

	/**
	 * Extracts positive integer value of the given configuration property.
	 * 
	 * @param propName
	 *            property name
	 * @param config
	 *            configuration properties
	 * @param defaultValue
	 *            the value returned in case when such property does not exist
	 * @return the property value or the passed default value, if such property does not exist
	 * @throws ConfigurationException
	 *             if the given property exists but contains invalid value
	 */
	public static int getPositiveIntegerValue(String propName, Map<String, String> config, int defaultValue)
			throws ConfigurationException {
		String strValue = config.get(propName);

		int intValue;
		if (NumberUtil.isPositiveInteger(strValue)) {
			intValue = Integer.parseInt(strValue);
		} else if (strValue == null) {
			intValue = defaultValue;
		} else {
			throw new ConfigurationException(
					String.format("The value of the configuration property [%s] must be a positive integer", propName));
		}

		return intValue;
	}
}
