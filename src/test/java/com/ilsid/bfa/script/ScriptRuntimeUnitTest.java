package com.ilsid.bfa.script;

import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.runtime.persistence.hibernate.HibernateRuntimeRepository;

public class ScriptRuntimeUnitTest extends BaseUnitTestCase {
	
	private ScriptRuntime runtime;
	
	@Before
	public void setUp() throws Exception {
		CodeRepositoryInitializer.init();
		runtime = new ScriptRuntime();
		runtime.setRepository(new HibernateRuntimeRepository());
	}
	
	@Test
	public void scriptCanBeRun() throws Exception {
		long runtimeId = runtime.runScript("Script001");
		assertTrue(runtimeId > 0);
	}
	
	@Test
	public void scriptWithSubflowCanBeRun() throws Exception {
		long runtimeId = runtime.runScript("SingleSubflowScript");
		assertTrue(runtimeId > 0);
	}

}
