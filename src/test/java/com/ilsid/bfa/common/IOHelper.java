package com.ilsid.bfa.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class IOHelper {
	
	private static final String SCRIPTS_DIR = "src/test/resources/dynamicCode/";
	
	private static final String BYTECODE_DIR = "src/test/resources/byteCode/";
	
	public static String loadScript(String fileName) throws Exception {
		String result;
		try (InputStream is = new FileInputStream(new File(SCRIPTS_DIR + fileName));) {
			result = IOUtils.toString(is);
		}
		
		return result;
	}
	
	public static byte[] loadClass(String fileName) throws Exception {
		byte[] result;

		try (InputStream is = new FileInputStream(BYTECODE_DIR + fileName)) {
			result = IOUtils.toByteArray(is);
		}

		return result;
	}

}
