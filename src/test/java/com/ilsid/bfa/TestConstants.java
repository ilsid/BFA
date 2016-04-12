package com.ilsid.bfa;

import java.io.File;

public class TestConstants {

	public final static String DUMMY_EXPRESSION_RESULT = "Dummy ExprParam Result";

	public final static String DUMMY_SCRIPT_RESULT = "Dummy Script Result";

	public final static String TEST_RESOURCES_DIR = "src/test/resources";

	public final static String CODE_REPOSITORY_DIR = TEST_RESOURCES_DIR + "/code_repository";

	public final static File INIT_DATABASE_DIR = new File(TestConstants.TEST_RESOURCES_DIR,
			"database/orientdb/databases");
}
