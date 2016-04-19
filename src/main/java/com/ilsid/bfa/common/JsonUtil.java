package com.ilsid.bfa.common;

import java.io.File;
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

	private static final TypeReference<LinkedHashMap<String, String>> MAP_TYPE_REF = new TypeReference<LinkedHashMap<String, String>>() {
	};

	/**
	 * Converts JSON string to {@link Map} instance.
	 * 
	 * @param json
	 *            JSON string
	 * @return {@link Map} representation
	 * @throws IOException
	 *             in case the string can't be parsed
	 */
	public static Map<String, String> toMap(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, MAP_TYPE_REF);
	}

	/**
	 * Converts JSON file to {@link Map} instance.
	 * 
	 * @param file
	 *            JSON file
	 * @return {@link Map} representation
	 * @throws IOException
	 *             in case the file can't be parsed
	 */
	public static Map<String, String> toMap(File file) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(file, MAP_TYPE_REF);
	}

	/**
	 * Converts {@link Map} instance to JSON string.
	 * 
	 * @param map
	 *            a map to convert
	 * @return JSON string
	 * @throws IOException
	 *             in case a map can't be converted
	 */
	public static String toJsonString(Map<String, String> map) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(map);
	}
}
