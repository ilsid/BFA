package com.ilsid.bfa.action;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

public class ActionContextUnitTest extends BaseUnitTestCase {

	@Test
	public void sameInstanceIsReturnedOnConsequentCallsInSingleThread() {
		final ActionContext inst1 = ActionContext.getInstance();
		final ActionContext inst2 = ActionContext.getInstance();
		final ActionContext inst3 = ActionContext.getInstance();

		assertSame(inst1, inst2);
		assertSame(inst2, inst3);
	}

	@Test
	public void newInstanceIsReturnedAfterCleanup() {
		final ActionContext inst1 = ActionContext.getInstance();
		ActionContext.cleanup();
		final ActionContext inst2 = ActionContext.getInstance();
		ActionContext.cleanup();
		final ActionContext inst3 = ActionContext.getInstance();
		ActionContext.cleanup();

		assertNotSame(inst1, inst2);
		assertNotSame(inst2, inst3);
	}

	@Test
	public void newInstanceIsReturnedForEachThread() throws Exception {
		final List<ActionContext> instances = new CopyOnWriteArrayList<>();

		final ActionContext inst11 = ActionContext.getInstance();
		instances.add(inst11);

		final Thread thread1 = new Thread(new ActionContextUser(instances));
		final Thread thread2 = new Thread(new ActionContextUser(instances));

		thread1.start();
		thread2.start();
		thread1.join();
		thread2.join();

		final ActionContext inst12 = ActionContext.getInstance();
		assertSame(inst11, inst12);

		assertNotSame(instances.get(0), instances.get(1));
		assertNotSame(instances.get(1), instances.get(2));
	}

	private class ActionContextUser implements Runnable {

		private final List<ActionContext> instances;

		public ActionContextUser(List<ActionContext> instances) {
			this.instances = instances;
		}

		public void run() {
			final ActionContext inst = ActionContext.getInstance();
			instances.add(inst);
			ActionContextUnitTest.this.assertSame(inst, ActionContext.getInstance());
		}

	}

}
