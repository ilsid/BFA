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

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
		return OBJECT_MAPPER.readValue(json, MAP_TYPE_REF);
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
		return OBJECT_MAPPER.readValue(file, MAP_TYPE_REF);
	}

	/**
	 * Converts an object to JSON string.
	 * 
	 * @param obj
	 *            an object to convert
	 * @return JSON string
	 * @throws IOException
	 *             in case an object can't be converted
	 */
	public static String toJsonString(Object obj) throws IOException {
		return OBJECT_MAPPER.writeValueAsString(obj);
	}

	/**
	 * Converts JSON string to an object of the specified type.
	 * 
	 * @param json
	 *            JSON string
	 * @param type
	 *            object's type
	 * @return
	 * @throws IOException
	 *             in case the string can't be converted
	 */
	public static <T> T toObject(String json, Class<T> type) throws IOException {
		return OBJECT_MAPPER.readValue(json, type);
	}

	/**
	 * Checks whether a passed content is a valid JSON string.
	 * 
	 * @param content
	 *            string to examine
	 * @return <code>true</code> if content is valid and <code>false</code> otherwise
	 */
	public static boolean isValidJsonString(String content) {
		try {
			OBJECT_MAPPER.readTree(content);
		} catch (IOException e) {
			return false;
		}

		return true;
	}
}
