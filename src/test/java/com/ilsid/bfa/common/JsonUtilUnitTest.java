package com.ilsid.bfa.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.test.types.Contract;
import com.ilsid.bfa.test.types.ContractHolder;

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

	@Test
	public void mapCanBeConvertedToJsonString() throws Exception {
		@SuppressWarnings("serial")
		Map<String, String> map = new LinkedHashMap<String, String>() {
			{
				put("var1", "Integer");
				put("var2", "Double");
				put("var3", "String");
			}
		};

		assertEquals("{\"var1\":\"Integer\",\"var2\":\"Double\",\"var3\":\"String\"}", JsonUtil.toJsonString(map));
	}

	@Test
	public void objectCanBeConvertedToJsonString() throws Exception {
		List<SimpleObject> list = new LinkedList<>();
		list.add(new SimpleObject("aaa", 1));
		list.add(new SimpleObject("bbb", 2));
		ComplexObject obj = new ComplexObject();
		obj.setStrField("ccc");
		obj.setSomeList(list);

		assertEquals(
				"{\"strField\":\"ccc\",\"someList\":[{\"field1\":\"aaa\",\"field2\":1},{\"field1\":\"bbb\",\"field2\":2}]}",
				JsonUtil.toJsonString(obj));
	}

	@Test
	public void jsonStringCanBeConvertedToObject() throws Exception {
		String json = "{\"Days\":\"33\",\"MonthlyFee\":\"77.99\",\"ID\":\"abc\"}";
		Contract contract = JsonUtil.toObject(json, Contract.class);

		assertEquals(33, contract.Days);
		assertEquals(77.99, contract.MonthlyFee);
		assertEquals("abc", contract.ID);
	}

	@Test
	public void jsonStringCanBeConvertedToComposedObject() throws Exception {
		String json = "{\"ID\":\"44\", \"Contract\":{\"Days\":\"55\",\"MonthlyFee\":\"77.99\"}}";
		ContractHolder holder = JsonUtil.toObject(json, ContractHolder.class);

		assertEquals(44, holder.ID);
		assertEquals(55, holder.Contract.Days);
		assertEquals(77.99, holder.Contract.MonthlyFee);
	}

	@Test
	public void invalidJsonStringIsNotRecognized() {
		assertFalse(JsonUtil.isValidJsonString(""));
		assertFalse(JsonUtil.isValidJsonString("abc"));
		assertFalse(JsonUtil.isValidJsonString("{name:value}"));
		assertFalse(JsonUtil.isValidJsonString("{"));
		assertFalse(JsonUtil.isValidJsonString("}"));
		// No closing bracket
		assertFalse(JsonUtil.isValidJsonString("{\"Days\":\"33\",\"MonthlyFee\":\"77.99\""));
	}

	@Test
	public void validJsonStringIsRecognized() {
		assertTrue(JsonUtil.isValidJsonString("{}"));
		assertTrue(JsonUtil.isValidJsonString("{\"Days\":\"33\",\"MonthlyFee\":\"77.99\"}"));
		assertTrue(
				JsonUtil.isValidJsonString("{\"ID\":\"44\", \"Contract\":{\"Days\":\"55\",\"MonthlyFee\":\"77.99\"}}"));
	}

}
