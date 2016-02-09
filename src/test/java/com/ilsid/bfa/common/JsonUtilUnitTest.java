package com.ilsid.bfa.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

public class JsonUtilUnitTest extends BaseUnitTestCase {

	@Test
	public void validJsonStringCanBeConvertedToMap() throws Exception {
		Map<String, String> map = JsonUtil
				.toMap("{\"field1\":\"value1\", \"field2\":\"value2\", \"field3\":\"complex value\"}");
		assertEquals(3, map.size());
		assertEquals("value1", map.get("field1"));
		assertEquals("value2", map.get("field2"));
		assertEquals("complex value", map.get("field3"));
	}

	@Test
	public void convertedMapPreservesFieldsOrder() throws Exception {
		Map<String, String> map = JsonUtil
				.toMap("{\"c\":\"value1\", \"b\":\"value2\", \"a\":\"value3\", \"2\":\"value4\" , \"1\":\"value5\"}");
		assertEquals(5, map.size());
		List<String> fields = new ArrayList<>();
		fields.addAll(map.keySet());
		assertEquals("c", fields.get(0));
		assertEquals("b", fields.get(1));
		assertEquals("a", fields.get(2));
		assertEquals("2", fields.get(3));
		assertEquals("1", fields.get(4));
	}

}
