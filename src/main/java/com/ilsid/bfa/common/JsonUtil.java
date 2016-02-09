package com.ilsid.bfa.common;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 * Provides JSON processing routines.
 * 
 * @author illia.sydorovych
 *
 */
public class JsonUtil {

	/**
	 * Converts JSON string into {@link Map} instance.
	 * 
	 * @param json
	 *            JSON string
	 * @return {@link Map} representation
	 * @throws IOException
	 *             in case the string can't be parsed
	 */
	public static Map<String, String> toMap(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<LinkedHashMap<String, String>> typeRef = new TypeReference<LinkedHashMap<String, String>>() {
		};
		LinkedHashMap<String, String> result = mapper.readValue(json, typeRef);

		return result;
	}

}
