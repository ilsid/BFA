package com.ilsid.bfa.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.ScriptingRepository;
import com.ilsid.bfa.persistence.TransactionManager;
import com.ilsid.bfa.script.ClassCompilationException;
import com.ilsid.bfa.script.ClassCompiler;
import com.ilsid.bfa.script.CompilationBlock;
import com.ilsid.bfa.script.CompilerConstants;
import com.ilsid.bfa.script.TypeNameResolver;

/**
 * Provides a set of management operations for scripts.
 * 
 * @author illia.sydorovych
 *
 */
// TODO: write unit tests
public class ScriptManager {

	private static final String METADATA_VALUE_SCRIPT_TYPE = "SCRIPT";

	private ScriptingRepository repository;

	/**
	 * Creates new script in the repository. If no group is defined then the script is to be saved in the Default Group.
	 * 
	 * @param scriptName
	 *            script name
	 * @param scriptBody
	 *            script body
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if the script's group does not exists in the repository</li>
	 *             <li>if the script itself or any of its expressions can't be compiled or persisted</li>
	 *             <li>if the script with such name already exists within the given group</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public void createScript(String scriptName, String scriptBody) throws ManagementException {
		checkParentScriptGroupExists(scriptName);

		ScriptCompilationUnit compilationUnit = compileScript(scriptName, scriptBody);
		try {
			startTransaction();
			saveScript(compilationUnit, scriptBody);
			saveScriptMetadata(compilationUnit);
			commitTransaction();
		} catch (PersistenceException e) {
			rollbackTransaction();
			throw new ManagementException(String.format("Failed to create the script [%s]", scriptName), e);
		}
	}

	/**
	 * Updates the existing script in the repository. If no group is defined then the script is searched in the Default
	 * Group.
	 * 
	 * @param scriptName
	 *            the name of the script to update
	 * @param scriptBody
	 *            the modified script body
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if the script's group does not exists in the repository</li>
	 *             <li>if the script itself or any of its expressions can't be compiled or persisted</li>
	 *             <li>if the script with such name does not exist in the repository within the given group</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public void updateScript(String scriptName, String scriptBody) throws ManagementException {
		checkParentScriptGroupExists(scriptName);

		ScriptCompilationUnit compilationUnit = compileScript(scriptName, scriptBody);
		try {
			startTransaction();
			deleteScript(scriptName);
			saveScript(compilationUnit, scriptBody);
			saveScriptMetadata(compilationUnit);
			commitTransaction();
		} catch (PersistenceException e) {
			rollbackTransaction();
			throw new ManagementException(String.format("Failed to update the script [%s]", scriptName), e);
		}

		DynamicClassLoader.reloadClasses();
	}

	/**
	 * Loads the body of the given script from the repository. If no group is defined then the script is searched in the
	 * Default Group.
	 * 
	 * @param scriptName
	 *            the name of the script to load
	 * @return the script body
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if the script's group does not exists in the repository</li>
	 *             <li>if the script with such name does not exist in the repository within the Default Group</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public String getScriptSourceCode(String scriptName) throws ManagementException {
		checkParentScriptGroupExists(scriptName);

		String className = TypeNameResolver.resolveScriptClassName(scriptName);
		String body;
		try {
			body = repository.loadSourceCode(className);
		} catch (PersistenceException e) {
			throw new ManagementException(String.format("Failed to load a source code of the script [%s]", scriptName),
					e);
		}

		if (body == null) {
			throw new ManagementException(
					String.format("The script [%s] does not exist in the repository", scriptName));
		}

		return body;
	}

	/**
	 * Creates new entity in the repository. The entity belongs to the Default Group.</br>
	 * The following entity body format is expected:</br>
	 * </br>
	 * <code>type name[;type name[;type name[...]]]</code> </br>
	 * </br>
	 * The entity body example: </br>
	 * </br>
	 * <code>Numeric Days;Numeric ProlongDays;Decimal MonthlyFee</code>
	 * 
	 * @param entityName
	 *            entity name
	 * @param entityBody
	 *            entity body
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if the entity body format is not valid</li>
	 *             <li>if the entity with such name already exists in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public void createEntity(String entityName, String entityBody) throws ManagementException {
		EntityCompilationUnit compilationUnit = compileEntity(entityName, entityBody);
		saveEntity(compilationUnit, entityBody);
	}

	/**
	 * Updates the existing entity in the repository. The entity is searched in the Default Group.
	 * 
	 * @param entityName
	 *            the name of the entity to update
	 * @param entityBody
	 *            the modified entity body
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if the entity can't be compiled or persisted</li>
	 *             <li>if the entity with such name does not exist in the repository within the Default Group</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public void updateEntity(String entityName, String entityBody) throws ManagementException {
		EntityCompilationUnit compilationUnit = compileEntity(entityName, entityBody);
		try {
			startTransaction();
			deleteEntity(entityName);
			saveEntity(compilationUnit, entityBody);
			commitTransaction();
		} catch (PersistenceException e) {
			rollbackTransaction();
			throw new ManagementException(String.format("Failed to update the entity [%s]", entityName), e);
		}

		DynamicClassLoader.reloadClasses();
	}

	/**
	 * Loads the body of the given entity from the repository. The entity is searched in the Default Group.
	 * 
	 * @param entityName
	 *            the name of the entity to load
	 * @return the entity body
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if the entity with such name does not exist in the repository within the Default Group</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public String getEntitySourceCode(String entityName) throws ManagementException {
		String className = TypeNameResolver.resolveEntityClassName(entityName);
		String body;
		try {
			body = repository.loadSourceCode(className);
		} catch (PersistenceException e) {
			throw new ManagementException(String.format("Failed to load a source code of the entity [%s]", entityName),
					e);
		}

		if (body == null) {
			throw new ManagementException(
					String.format("The entity [%s] does not exist in the repository", entityName));
		}

		return body;
	}

	/**
	 * Loads meta-data items for all top-level script groups.
	 * 
	 * @return a list of meta-data items
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if no top-level groups exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public List<Map<String, String>> getTopLevelGroupMetadatas() throws ManagementException {
		List<Map<String, String>> result;
		try {
			result = repository.loadMetadataForTopLevelPackages();
		} catch (PersistenceException e) {
			throw new ManagementException("Failed to load the info for top-level script groups", e);
		}

		if (result.isEmpty()) {
			throw new ManagementException("No top-level script groups found");
		}

		addParentInfo(result, Metadata.ROOT_PARENT_NAME);

		return result;
	}

	/**
	 * Loads meta-data items for sub-groups and scripts in the specified group.
	 * 
	 * @param groupName
	 *            a group name
	 * @return a list of meta-data items or an empty list, if no sub-groups and scripts found or such group does not
	 *         exist
	 * @throws ManagementException
	 *             in case of any repository access issues
	 */
	public List<Map<String, String>> getChildrenMetadatas(String groupName) throws ManagementException {
		List<Map<String, String>> result;
		String packageName = TypeNameResolver.resolveScriptGroupPackageName(groupName);
		try {
			result = repository.loadMetadataForChildPackages(packageName);
		} catch (PersistenceException e) {
			throw new ManagementException(
					String.format("Failed to load child items info from the group [%s]", groupName), e);
		}

		if (!result.isEmpty()) {
			addParentInfo(result, groupName);
		}

		return result;
	}

	/**
	 * Creates new script group.
	 * 
	 * @param groupName
	 *            the script group name
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if such group already exists in the repository</li>
	 *             <li>if parent group does not exists in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public void createScriptGroup(String groupName) throws ManagementException {
		String parentGroupName = TypeNameResolver.splitGroupName(groupName).getParentName();
		if (parentGroupName != null) {
			checkParentScriptGroupExists(groupName);
		}

		String packageName = TypeNameResolver.resolveScriptGroupPackageName(groupName);
		try {
			repository.savePackage(packageName, createScriptGroupMetadata(groupName));
		} catch (PersistenceException e) {
			throw new ManagementException(String.format("Failed to create the script group [%s]", groupName), e);
		}
	}

	/**
	 * Creates new entity group.
	 * 
	 * @param groupName
	 *            the entity group name
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if such group already exists in the repository</li>
	 *             <li>if parent group does not exists in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public void createEntityGroup(String groupName) throws ManagementException {
		String parentGroupName = TypeNameResolver.splitGroupName(groupName).getParentName();
		if (parentGroupName != null) {
			checkParentEntityGroupExists(groupName);
		}

		String packageName = TypeNameResolver.resolveEntityGroupPackageName(groupName);
		try {
			repository.savePackage(packageName, createEntityGroupMetadata(groupName));
		} catch (PersistenceException e) {
			throw new ManagementException(String.format("Failed to create the entity group [%s]", groupName), e);
		}
	}

	/**
	 * Defines a code repository implementation.
	 * 
	 * @param repository
	 *            a code repository
	 */
	@Inject
	public void setRepository(ScriptingRepository repository) {
		this.repository = repository;
	}

	private void saveScript(ScriptCompilationUnit compilationUnit, String scriptBody) throws ManagementException {
		try {
			repository.save(compilationUnit.scriptClassName, compilationUnit.scriptByteCode, scriptBody);

			for (CompilationBlock expr : compilationUnit.scriptExpressions) {
				repository.save(expr.getClassName(), expr.getByteCode());
			}
		} catch (PersistenceException e) {
			rollbackTransaction();
			throw new ManagementException(
					String.format("Failed to persist the script [%s]", compilationUnit.scriptName), e);
		}
	}

	private void saveScriptMetadata(ScriptCompilationUnit compilationUnit) throws PersistenceException {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put(Metadata.TYPE, METADATA_VALUE_SCRIPT_TYPE);
		metaData.put(Metadata.NAME, compilationUnit.scriptName);
		metaData.put(Metadata.TITLE, TypeNameResolver.splitName(compilationUnit.scriptName).getChildName());

		repository.saveMetadata(compilationUnit.scriptClassName, metaData);
	}

	private void deleteScript(String scriptName) throws ManagementException {
		String scriptClassName = TypeNameResolver.resolveScriptClassName(scriptName);
		String scriptPackageName = ClassNameUtil.getPackageName(scriptClassName);

		int deletedCnt;
		try {
			deletedCnt = repository.deletePackage(scriptPackageName);
		} catch (PersistenceException e) {
			rollbackTransaction();
			throw new ManagementException(
					String.format("Failed to delete the script's package [%s] from the repository", scriptPackageName),
					e);
		}

		if (deletedCnt == 0) {
			rollbackTransaction();
			throw new ManagementException(
					String.format("The script [%s] does not exist in the repository", scriptName));
		}
	}

	private ScriptCompilationUnit compileScript(String scriptName, String scriptBody) throws ManagementException {
		String scriptClassName = TypeNameResolver.resolveScriptClassName(scriptName);
		byte[] scriptByteCode;
		Collection<CompilationBlock> expressions;
		try {
			scriptByteCode = ClassCompiler.compileScript(scriptClassName, scriptBody);

			String scriptShortClassName = ClassNameUtil.getShortClassName(scriptClassName);
			final String scriptSourceCode = String.format(CompilerConstants.SCRIPT_SOURCE_TEMPLATE,
					ClassNameUtil.getPackageName(scriptClassName), scriptShortClassName, scriptBody);

			expressions = ClassCompiler.compileScriptExpressions(scriptSourceCode);
		} catch (ClassCompilationException e) {
			throw new ManagementException(String.format("Compilation of the script [%s] failed", scriptName), e);
		}

		ScriptCompilationUnit result = new ScriptCompilationUnit();
		result.scriptName = scriptName;
		result.scriptClassName = scriptClassName;
		result.scriptByteCode = scriptByteCode;
		result.scriptExpressions = expressions;

		return result;
	}

	private String validateAndTransformEntityBody(String entityName, String entityBody) throws ManagementException {
		String[] fieldExpressions = entityBody.split(";");
		StringBuilder javaCode = new StringBuilder();
		for (String fieldExpr : fieldExpressions) {
			String trimmedExpr = fieldExpr.trim();
			String[] exprParts = trimmedExpr.split("\\s+");
			if (exprParts.length != 2) {
				throw new ManagementException(
						String.format("The entity [%s] contains invalid expression [%s]", entityName, trimmedExpr));
			}
			String javaType = TypeNameResolver.resolveEntityClassName(exprParts[0]);
			javaCode.append(javaType).append(" ").append(exprParts[1]).append(";");
		}

		return javaCode.toString();
	}

	private EntityCompilationUnit compileEntity(String entityName, String entityBody) throws ManagementException {
		String bodyJavaCode = validateAndTransformEntityBody(entityName, entityBody);
		String className = TypeNameResolver.resolveEntityClassName(entityName);

		byte[] byteCode;
		try {
			byteCode = ClassCompiler.compileEntity(className, bodyJavaCode);
		} catch (ClassCompilationException e) {
			throw new ManagementException(String.format("Compilation of the entity [%s] failed", entityName), e);
		}

		EntityCompilationUnit compilationUnit = new EntityCompilationUnit();
		compilationUnit.entityName = entityName;
		compilationUnit.entityClassName = className;
		compilationUnit.entityByteCode = byteCode;

		return compilationUnit;
	}

	private void saveEntity(EntityCompilationUnit compilationUnit, String entityBody) throws ManagementException {
		try {
			repository.save(compilationUnit.entityClassName, compilationUnit.entityByteCode, entityBody);
		} catch (PersistenceException e) {
			throw new ManagementException(String.format("Failed to create the entity [%s]", compilationUnit.entityName),
					e);
		}
	}

	private void deleteEntity(String entityName) throws ManagementException {
		String className = TypeNameResolver.resolveEntityClassName(entityName);

		int deletedCnt;
		try {
			deletedCnt = repository.deleteClass(className);
		} catch (PersistenceException e) {
			throw new ManagementException(
					String.format("Failed to delete the entity [%s] from the repository", entityName), e);
		}

		if (deletedCnt == 0) {
			rollbackTransaction();
			throw new ManagementException(
					String.format("The entity [%s] does not exist in the repository", entityName));
		}
	}

	private void addParentInfo(List<Map<String, String>> metaDatas, String parentName) {
		for (Map<String, String> metaData : metaDatas) {
			metaData.put(Metadata.PARENT, parentName);
		}
	}

	private Map<String, String> createScriptGroupMetadata(String groupName) throws ManagementException {
		Map<String, String> metaData = new HashMap<>();
		metaData.put(Metadata.TYPE, Metadata.SCRIPT_GROUP_TYPE);
		metaData.put(Metadata.NAME, groupName);
		metaData.put(Metadata.TITLE, TypeNameResolver.splitName(groupName).getChildName());

		return metaData;
	}

	private Map<String, String> createEntityGroupMetadata(String groupName) throws ManagementException {
		Map<String, String> metaData = new HashMap<>();
		metaData.put(Metadata.NAME, groupName);
		metaData.put(Metadata.TITLE, TypeNameResolver.splitName(groupName).getChildName());

		return metaData;
	}

	private void checkParentScriptGroupExists(String name) throws ManagementException {
		final String parentGroupName = TypeNameResolver.splitName(name).getParentName();
		String parentPackageName = TypeNameResolver.resolveScriptGroupPackageName(parentGroupName);
		Map<String, String> parentMetadata;
		try {
			parentMetadata = repository.loadMetadataForPackage(parentPackageName);
		} catch (PersistenceException e) {
			throw new ManagementException(
					String.format("Failed to load meta-data for the script group [%s]", parentGroupName), e);
		}

		if (!isScriptGroup(parentMetadata)) {
			throw new ManagementException(String.format("No parent script group [%s] exists", parentGroupName));
		}
	}

	private void checkParentEntityGroupExists(String name) throws ManagementException {
		final String parentGroupName = TypeNameResolver.splitGroupName(name).getParentName();

		if (parentGroupName == null) {
			return;
		}

		String parentPackageName = TypeNameResolver.resolveEntityGroupPackageName(parentGroupName);
		Map<String, String> parentMetadata;
		try {
			parentMetadata = repository.loadMetadataForPackage(parentPackageName);
		} catch (PersistenceException e) {
			throw new ManagementException(
					String.format("Failed to load meta-data for the entity group [%s]", parentGroupName), e);
		}

		if (parentMetadata == null) {
			throw new ManagementException(String.format("No parent entity group [%s] exists", parentGroupName));
		}
	}

	private boolean isScriptGroup(Map<String, String> meta) {
		if (meta != null && Metadata.SCRIPT_GROUP_TYPE.equals(meta.get(Metadata.TYPE))) {
			return true;
		}

		return false;
	}

	private void startTransaction() throws PersistenceException {
		repository.getTransactionManager().startTransaction();
	}

	private void commitTransaction() throws PersistenceException {
		repository.getTransactionManager().commitTransaction();
	}

	private void rollbackTransaction() {
		TransactionManager txManager = repository.getTransactionManager();
		if (txManager.isTransactionStarted()) {
			txManager.rollbackTransaction();
		}
	}

	private static class ScriptCompilationUnit {

		String scriptName;

		String scriptClassName;

		byte[] scriptByteCode;

		Collection<CompilationBlock> scriptExpressions;

	}

	private static class EntityCompilationUnit {

		String entityName;

		String entityClassName;

		byte[] entityByteCode;
	}

}
