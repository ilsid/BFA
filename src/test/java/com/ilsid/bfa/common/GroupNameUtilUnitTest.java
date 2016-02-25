package com.ilsid.bfa.common;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

public class GroupNameUtilUnitTest extends BaseUnitTestCase {

	private static final String DEFAULT_PARENT_NAME = "default_parent";

	@Test
	public void simpleNameCanBeSplit() {
		GroupNameUtil.NameParts parts = GroupNameUtil.splitName("Some Name");
		assertNull(parts.getParentName());
		assertEquals("Some Name", parts.getChildName());
	}

	@Test
	public void complexNameWithTwoPartsCanBeSplit() {
		GroupNameUtil.NameParts parts = GroupNameUtil.splitName("Some Parent::Some Child");
		assertEquals("Some Parent", parts.getParentName());
		assertEquals("Some Child", parts.getChildName());
	}

	@Test
	public void complexNameWithThreePartsCanBeSplit() {
		GroupNameUtil.NameParts parts = GroupNameUtil.splitName("Some Grand-Parent::Some Parent::Some Child");
		assertEquals("Some Grand-Parent::Some Parent", parts.getParentName());
		assertEquals("Some Child", parts.getChildName());
	}

	@Test
	public void simpleNameWithDefaultParentCanBeSplit() {
		GroupNameUtil.NameParts parts = GroupNameUtil.splitName("Some Name", DEFAULT_PARENT_NAME);
		assertEquals(DEFAULT_PARENT_NAME, parts.getParentName());
		assertEquals("Some Name", parts.getChildName());
	}

	@Test
	public void complexNameWithDefaultParentCanBeSplit() {
		GroupNameUtil.NameParts parts = GroupNameUtil.splitName("Some Parent::Some Name", DEFAULT_PARENT_NAME);
		assertEquals("Some Parent", parts.getParentName());
		assertEquals("Some Name", parts.getChildName());
	}

}
