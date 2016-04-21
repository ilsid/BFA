package com.ilsid.bfa;

import java.io.File;

public class IntegrationTestConstants {

	private final static String CODE_REPOSITORY_PATH = TestConstants.TEST_RESOURCES_DIR + "/__tmp_code_repository";

	public final static File CODE_REPOSITORY_DIR = new File(CODE_REPOSITORY_PATH);

	public final static File ORIENTDB_HOME_DIR = new File(CODE_REPOSITORY_DIR, "orientdb");
	
	public final static File DATABASE_DIR = new File(ORIENTDB_HOME_DIR, "databases");
	
}