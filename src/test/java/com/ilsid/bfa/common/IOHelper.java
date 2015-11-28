package com.ilsid.bfa.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.ilsid.bfa.TestConstants;

public class IOHelper {
	
	private static final String SCRIPTS_DIR = TestConstants.TEST_RESOURCES_DIR + "/dynamicCode/";
	
	private static final String BYTECODE_DIR = TestConstants.TEST_RESOURCES_DIR + "/byteCode/";
	
	public static String loadScript(String fileName) throws Exception {
		String result;
		try (InputStream is = new FileInputStream(new File(SCRIPTS_DIR + fileName));) {
			result = IOUtils.toString(is);
		}
		
		return result;
	}
	
	public static String loadFileContents(String dir, String fileName) throws Exception {
		String result;
		try (InputStream is = new FileInputStream(new File(dir + "/" + fileName));) {
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
	
	public static byte[] loadClass(String dir, String fileName) throws Exception {
		byte[] result;

		try (InputStream is = new FileInputStream(new File(dir + "/" + fileName))) {
			result = IOUtils.toByteArray(is);
		}

		return result;
	}

}
