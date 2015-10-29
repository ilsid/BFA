package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.common.CompileHelper;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.persistence.CodeRepository;

public class FSCodeRepositoryUnitTest extends BaseUnitTestCase {

	private static final String SCRIPT_SOURCE_FILE_NAME = "single-expression-script.txt";

	private final static String ROOT_DIR_PATH = "src/test/resources/__tmp_class_repository";

	private final static String SCRIPT_CLASS_NAME = CompileHelper.GENERATED_SCRIPT_PACKAGE
			+ FSCodeRepositoryUnitTest.class.getSimpleName() + "Script01";

	private final static String PATH_WO_EXTENSION = ROOT_DIR_PATH + "/" + SCRIPT_CLASS_NAME.replace('.', '/');

	private final static File SCRIPT_CLASS_FILE = new File(PATH_WO_EXTENSION + ".class");

	private final static File SCRIPT_SRC_FILE = new File(PATH_WO_EXTENSION + ".src");

	private final static File ROOT_DIR = new File(ROOT_DIR_PATH);

	private CodeRepository repository = new FSCodeRepository(ROOT_DIR_PATH);

	@Before
	public void beforeAllTests() throws IOException {
		FileUtils.forceMkdir(ROOT_DIR);
	}

	@After
	public void afterAllTests() throws IOException {
		FileUtils.forceDelete(ROOT_DIR);
	}

	@Test
	public void classOnlyCanBeSaved() throws Exception {
		saveClass();

		assertTrue(SCRIPT_CLASS_FILE.exists());
		assertFalse(SCRIPT_SRC_FILE.exists());
	}

	@Test
	public void classAndSourceCodeCanBeSaved() throws Exception {
		saveClassAndSource();

		assertTrue(SCRIPT_CLASS_FILE.exists());
		assertTrue(SCRIPT_SRC_FILE.exists());
		
		String savedScriptBodySource;
		try (InputStream savedScriptBody = new FileInputStream(SCRIPT_SRC_FILE)) {
			savedScriptBodySource = IOUtils.toString(savedScriptBody, "UTF-8");
		}
		
		String inputScriptBodySource;
		try (InputStream inputScriptBody = IOHelper.loadScript(SCRIPT_SOURCE_FILE_NAME);) {
			inputScriptBodySource = IOUtils.toString(inputScriptBody, "UTF-8");
		}
		
		assertEquals(inputScriptBodySource, savedScriptBodySource);
	}

	private void saveClassAndSource() throws Exception {
		saveClass(true);
	}

	private void saveClass() throws Exception {
		saveClass(false);
	}

	private void saveClass(boolean saveSource) throws Exception {
		String scriptBodySource;
		try (InputStream script = IOHelper.loadScript(SCRIPT_SOURCE_FILE_NAME);) {
			scriptBodySource = IOUtils.toString(script);
		}

		byte[] byteCode = CompileHelper.compileScript(SCRIPT_CLASS_NAME, IOUtils.toInputStream(scriptBodySource));

		if (saveSource) {
			repository.save(SCRIPT_CLASS_NAME, byteCode, scriptBodySource);
		} else {
			repository.save(SCRIPT_CLASS_NAME, byteCode);
		}
	}

}
