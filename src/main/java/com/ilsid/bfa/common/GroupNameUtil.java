package com.ilsid.bfa.common;

import java.io.File;

/**
 * Provides the routines for the group names processing.
 * 
 * @author illia.sydorovych
 *
 */
public class GroupNameUtil {

	/**
	 * The separator used for sub-group naming. For example, the group name can be
	 * <i>grand_parent_group::parent_group::group</i>.
	 */
	public static final String GROUP_SEPARATOR = "::";

	/**
	 * Splits group name.
	 * 
	 * @param name
	 *            group name.
	 * @return parent and child parts. If no parent is defined then <code>null</code> is returned as a parent part
	 */
	public static NameParts splitName(String name) {
		return splitName(name, null);
	}

	/**
	 * Splits group name.
	 * 
	 * @param name
	 *            group name.
	 * @param defaultParentGroup
	 *            default parent group name
	 * @return parent and child parts. If no parent is defined then a value defined by<code>defaultParentGroup</code>
	 *         parameter is returned as a parent part
	 */
	public static NameParts splitName(String name, String defaultParentGroup) {
		int childSepIdx = name.lastIndexOf(GROUP_SEPARATOR);
		final int offset = childSepIdx + GROUP_SEPARATOR.length();

		String parentName;
		String childName;
		if (childSepIdx > 0 && offset < name.length()) {
			parentName = name.substring(0, childSepIdx);
			childName = name.substring(offset);
		} else {
			parentName = defaultParentGroup;
			childName = name;
		}

		NamePartsHolder result = new NamePartsHolder();
		result.parentName = parentName;
		result.childName = childName;

		return result;
	}

	/**
	 * Returns directories string from the given group name. For example, the method returns the string
	 * <i>grand_parent/parent/child</i> for the group name <i>grand_parent::parent::child</i>.
	 * 
	 * @param groupName
	 *            group name
	 * @return directories for the given group
	 */
	public static String getDirs(String groupName) {
		String result = groupName;
		result = ClassNameUtil.escape(result).replace(GROUP_SEPARATOR, File.separator)
				.replaceAll(ClassNameUtil.WHITESPACE_REGEXP, ClassNameUtil.BLANK_CODE).toLowerCase();

		return result;
	}

	public interface NameParts {

		String getParentName();

		String getChildName();
	}

	private static class NamePartsHolder implements NameParts {

		private String parentName;

		private String childName;

		public String getParentName() {
			return parentName;
		}

		public String getChildName() {
			return childName;
		}

	}
}
