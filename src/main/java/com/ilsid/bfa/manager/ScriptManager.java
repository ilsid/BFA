package com.ilsid.bfa.manager;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.JsonUtil;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.ScriptingRepository;
import com.ilsid.bfa.persistence.TransactionManager;
import com.ilsid.bfa.persistence.filesystem.MetadataUtil;
import com.ilsid.bfa.script.ClassCompilationException;
import com.ilsid.bfa.script.ClassCompiler;
import com.ilsid.bfa.script.ClassCompiler.CompilationBlock;
import com.ilsid.bfa.script.ClassCompiler.ScriptExpressionsUnit;
import com.ilsid.bfa.script.CompilerConstants;
import com.ilsid.bfa.script.TypeNameResolver;

/**
 * Provides a set of management operations for scripts.
 * 
 * @author illia.sydorovych
 *
 */
// TODO: write unit tests
@Singleton
public class ScriptManager {

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
	 * JSON entity body format is expected:</br>
	 * </br>
	 * <code>{"name":"type"[, "name":"type"[, ...]]}</code> </br>
	 * </br>
	 * The entity body example: </br>
	 * </br>
	 * <code>{"Days":"Numeric", "ProlongDays":"Numeric", "MonthlyFee":"Decimal"}</code>
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
		checkParentEntityGroupExists(entityName);
		EntityCompilationUnit compilationUnit = compileEntity(entityName, entityBody);
		try {
			startTransaction();
			saveEntity(compilationUnit, entityBody);
			saveEntityMetadata(compilationUnit);
			commitTransaction();
		} catch (PersistenceException e) {
			rollbackTransaction();
			throw new ManagementException(String.format("Failed to create the entity [%s]", entityName), e);
		}
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
		checkParentEntityGroupExists(entityName);

		EntityCompilationUnit compilationUnit = compileEntity(entityName, entityBody);
		try {
			startTransaction();
			deleteEntity(entityName);
			saveEntity(compilationUnit, entityBody);
			saveEntityMetadata(compilationUnit);
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
		checkParentEntityGroupExists(entityName);

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
	public List<Map<String, String>> getTopLevelScriptGroupMetadatas() throws ManagementException {
		List<Map<String, String>> result;
		try {
			result = repository.loadMetadataForChildPackages(ClassNameUtil.GENERATED_SCRIPTS_ROOT_PACKAGE,
					Metadata.SCRIPT_GROUP_TYPE);
		} catch (PersistenceException e) {
			throw new ManagementException("Failed to load the info for top-level script groups", e);
		}

		if (result.isEmpty()) {
			throw new ManagementException("No top-level script groups found");
		}

		MetadataUtil.addParentRecord(result, Metadata.ROOT_PARENT_NAME);

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
	public List<Map<String, String>> getChildrenScriptGroupMetadatas(String groupName) throws ManagementException {
		List<Map<String, String>> result;
		String packageName = TypeNameResolver.resolveScriptGroupPackageName(groupName);
		try {
			result = repository.loadMetadataForChildPackages(packageName, Metadata.SCRIPT_GROUP_TYPE,
					Metadata.SCRIPT_TYPE);
		} catch (PersistenceException e) {
			throw new ManagementException(
					String.format("Failed to load child items info from the script group [%s]", groupName), e);
		}

		if (!result.isEmpty()) {
			MetadataUtil.addParentRecord(result, groupName);
		}

		return result;
	}

	/**
	 * Loads meta-data items for all top-level entity groups.
	 * 
	 * @return a list of meta-data items
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if no top-level groups exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public List<Map<String, String>> getTopLevelEntityGroupMetadatas() throws ManagementException {
		List<Map<String, String>> result;
		try {
			result = repository.loadMetadataForChildPackages(ClassNameUtil.GENERATED_ENTITIES_ROOT_PACKAGE,
					Metadata.ENTITY_GROUP_TYPE);
		} catch (PersistenceException e) {
			throw new ManagementException("Failed to load the info for top-level entity groups", e);
		}

		if (result.isEmpty()) {
			throw new ManagementException("No top-level entity groups found");
		}

		MetadataUtil.addParentRecord(result, Metadata.ROOT_PARENT_NAME);

		return result;
	}

	/**
	 * Loads meta-data items for sub-groups and entities in the specified group.
	 * 
	 * @param groupName
	 *            a group name
	 * @return a list of meta-data items or an empty list, if no sub-groups and entities found or such group does not
	 *         exist
	 * @throws ManagementException
	 *             in case of any repository access issues
	 */
	public List<Map<String, String>> getChildrenEntityGroupMetadatas(String groupName) throws ManagementException {
		List<Map<String, String>> result;
		String packageName = TypeNameResolver.resolveEntityGroupPackageName(groupName);
		try {
			result = repository.loadMetadataForChildPackages(packageName, Metadata.ENTITY_GROUP_TYPE);
			result.addAll(repository.loadMetadataForClasses(packageName, Metadata.ENTITY_TYPE));
		} catch (PersistenceException e) {
			throw new ManagementException(
					String.format("Failed to load child items info from the entity group [%s]", groupName), e);
		}

		if (!result.isEmpty()) {
			MetadataUtil.addParentRecord(result, groupName);
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
		metaData.put(Metadata.TYPE, Metadata.SCRIPT_TYPE);
		metaData.put(Metadata.NAME, compilationUnit.scriptName);
		metaData.put(Metadata.TITLE, TypeNameResolver.splitName(compilationUnit.scriptName).getChildName());

		writeInputParametersIfAny(metaData, compilationUnit);

		String packageName = ClassNameUtil.getPackageName(compilationUnit.scriptClassName);
		repository.savePackageMetadata(packageName, metaData);
	}

	private void writeInputParametersIfAny(Map<String, String> metaData, ScriptCompilationUnit compilationUnit)
			throws PersistenceException {

		if (compilationUnit.scriptParameters.size() > 0) {
			try {
				metaData.put(Metadata.PARAMETERS, JsonUtil.toJsonString(compilationUnit.scriptParameters));
			} catch (IOException e) {
				throw new PersistenceException(
						String.format("Failed to save \"input parameters\" meta-data for the script [%s]",
								compilationUnit.scriptName),
						e);
			}
		}

	}

	private void saveEntityMetadata(EntityCompilationUnit compilationUnit) throws PersistenceException {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put(Metadata.TYPE, Metadata.ENTITY_TYPE);
		metaData.put(Metadata.NAME, compilationUnit.entityName);
		metaData.put(Metadata.TITLE, TypeNameResolver.splitName(compilationUnit.entityName).getChildName());

		repository.saveMetadata(compilationUnit.entityClassName, metaData);
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
		ScriptExpressionsUnit expressionsUnit;
		try {
			scriptByteCode = ClassCompiler.compileScript(scriptClassName, scriptBody);

			String scriptShortClassName = ClassNameUtil.getShortClassName(scriptClassName);
			final String scriptSourceCode = String.format(CompilerConstants.SCRIPT_SOURCE_TEMPLATE,
					ClassNameUtil.getPackageName(scriptClassName), scriptShortClassName, scriptBody);

			expressionsUnit = ClassCompiler.compileScriptExpressions(scriptSourceCode);
		} catch (ClassCompilationException e) {
			throw new ManagementException(String.format("Compilation of the script [%s] failed", scriptName), e);
		}

		ScriptCompilationUnit result = new ScriptCompilationUnit();
		result.scriptName = scriptName;
		result.scriptParameters = expressionsUnit.getInputParameters();
		result.scriptClassName = scriptClassName;
		result.scriptByteCode = scriptByteCode;
		result.scriptExpressions = expressionsUnit.getExpressions();

		return result;
	}

	private String transformToJavaCode(String entityName, String entityBody) throws ManagementException {
		Map<String, String> fields;
		try {
			fields = JsonUtil.toMap(entityBody);
		} catch (IOException e) {
			throw new ManagementException(
					String.format("The representation of entity [%s] has invalid format", entityName));
		}

		StringBuilder javaCode = new StringBuilder();
		for (String fieldName : fields.keySet()) {
			String fieldType = fields.get(fieldName);
			String javaType = TypeNameResolver.resolveEntityClassName(fieldType);
			javaCode.append(javaType).append(" ").append(fieldName).append(";");
		}

		return javaCode.toString();
	}

	private EntityCompilationUnit compileEntity(String entityName, String entityBody) throws ManagementException {
		String bodyJavaCode = transformToJavaCode(entityName, entityBody);
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

	private Map<String, String> createScriptGroupMetadata(String groupName) throws ManagementException {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put(Metadata.TYPE, Metadata.SCRIPT_GROUP_TYPE);
		metaData.put(Metadata.NAME, groupName);
		metaData.put(Metadata.TITLE, TypeNameResolver.splitGroupName(groupName).getChildName());

		return metaData;
	}

	private Map<String, String> createEntityGroupMetadata(String groupName) throws ManagementException {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put(Metadata.TYPE, Metadata.ENTITY_GROUP_TYPE);
		metaData.put(Metadata.NAME, groupName);
		metaData.put(Metadata.TITLE, TypeNameResolver.splitGroupName(groupName).getChildName());

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

		if (!isTypeOf(parentMetadata, Metadata.SCRIPT_GROUP_TYPE)) {
			throw new ManagementException(String.format("No parent script group [%s] exists", parentGroupName));
		}
	}

	private void checkParentEntityGroupExists(String name) throws ManagementException {
		final String parentGroupName = TypeNameResolver.splitName(name).getParentName();
		String parentPackageName = TypeNameResolver.resolveEntityGroupPackageName(parentGroupName);
		Map<String, String> parentMetadata;
		try {
			parentMetadata = repository.loadMetadataForPackage(parentPackageName);
		} catch (PersistenceException e) {
			throw new ManagementException(
					String.format("Failed to load meta-data for the entity group [%s]", parentGroupName), e);
		}

		if (!isTypeOf(parentMetadata, Metadata.ENTITY_GROUP_TYPE)) {
			throw new ManagementException(String.format("No parent entity group [%s] exists", parentGroupName));
		}
	}

	private boolean isTypeOf(Map<String, String> meta, String groupType) {
		if (meta != null && groupType.equals(meta.get(Metadata.TYPE))) {
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

		Map<String, String> scriptParameters;

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
