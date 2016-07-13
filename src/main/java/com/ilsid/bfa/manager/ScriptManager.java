package com.ilsid.bfa.manager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.ConfigurationException;
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
import com.ilsid.bfa.script.ClassCompiler.ScriptCompilationUnit;
import com.ilsid.bfa.script.TypeNameResolver;

/**
 * Provides a set of management operations for scripts.
 * 
 * @author illia.sydorovych
 *
 */
// TODO: write unit tests
@Singleton
public class ScriptManager extends AbstractManager implements Configurable {

	private static final String TMP_ENTITIES_JAR_MANIFEST_VERSION = "1.0";

	private static final String TMP_ENTITIES_JAR_NAME_TEMPLATE = "__tmp-bfa-entities-%s.jar";

	private static final String CONFIG_PROP_TMP_DIR_NAME = "bfa.tmp_dir";

	private static final String DEFAULT_TMP_DIR = "bfa";

	private static final Map<String, String> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<String, String>());

	private ScriptingRepository repository;

	private File tmpDir;

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

		ScriptUnit compilationUnit = compileScript(scriptName, scriptBody);
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

		ScriptUnit compilationUnit = compileScript(scriptName, scriptBody);
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
	 *             <li>if the script with such name does not exist in the repository</li>
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
	 * Creates new entity in the repository.</br>
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
		EntityUnit compilationUnit = compileEntity(entityName, entityBody);
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
	 * Updates the existing entity in the repository.
	 * 
	 * @param entityName
	 *            the name of the entity to update
	 * @param entityBody
	 *            the modified entity body
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if the entity can't be compiled or persisted</li>
	 *             <li>if the entity with such name does not exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public void updateEntity(String entityName, String entityBody) throws ManagementException {
		checkParentEntityGroupExists(entityName);

		EntityUnit compilationUnit = compileEntity(entityName, entityBody);
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
	 * Loads the body of the given entity from the repository.
	 * 
	 * @param entityName
	 *            the name of the entity to load
	 * @return the entity body
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if the entity with such name does not exist in the repository</li>
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
	 * Loads info about script input parameters (Name/Type entries).
	 * 
	 * @param scriptName
	 *            script name
	 * @return the input parameters meta-data represented as {@link Map} with Name/Type entries or an empty {@link Map}
	 *         if the script has no input parameters
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if the script with such name does not exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public Map<String, String> getScriptParametersMetadata(String scriptName) throws ManagementException {
		String className = TypeNameResolver.resolveScriptClassName(scriptName);
		String packageName = ClassNameUtil.getPackageName(className);

		Map<String, String> meta;
		try {
			meta = repository.loadMetadataForPackage(packageName);
		} catch (PersistenceException e) {
			throw new ManagementException(String.format("Failed to load meta-data for the script [%s]", scriptName), e);
		}

		if (meta == null) {
			throw new ManagementException(
					String.format("The script [%s] does not exist in the repository", scriptName));
		}

		Map<String, String> result = EMPTY_MAP;

		if (!meta.isEmpty() && isTypeOf(meta, Metadata.SCRIPT_TYPE)) {
			result = extractInputParameters(meta, scriptName);
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
			startTransaction();
			repository.savePackage(packageName, createScriptGroupMetadata(groupName));
			commitTransaction();
		} catch (PersistenceException e) {
			rollbackTransaction();
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
			startTransaction();
			repository.savePackage(packageName, createEntityGroupMetadata(groupName));
			commitTransaction();
		} catch (PersistenceException e) {
			rollbackTransaction();
			throw new ManagementException(String.format("Failed to create the entity group [%s]", groupName), e);
		}
	}

	/**
	 * Returns JAR archive with all Entity classes from the repository.
	 * 
	 * @return {@link InputStream} instance for JAR archive
	 * @throws ManagementException
	 *             in case of any repository access issues
	 */
	public InputStream getEnitiesLibrary() throws ManagementException {
		List<Entry<String, byte[]>> entityClasses;
		try {
			entityClasses = repository.loadClasses(ClassNameUtil.GENERATED_ENTITIES_ROOT_PACKAGE);
		} catch (PersistenceException e) {
			throw new ManagementException("Failed to load entity classes", e);
		}

		File tempJar = createTempJar(entityClasses);
		byte[] jarContent;
		try {
			jarContent = FileUtils.readFileToByteArray(tempJar);
		} catch (IOException e) {
			throw new ManagementException("Failed to get entities library", e);
		} finally {
			FileUtils.deleteQuietly(tempJar);
		}

		return new ByteArrayInputStream(jarContent);
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

	/**
	 * Defines configuration.
	 * 
	 * @param config
	 */
	@Override
	@Inject
	public void setConfiguration(@ManagerConfig Map<String, String> config) throws ConfigurationException {
		String tmpDirName = config.get(CONFIG_PROP_TMP_DIR_NAME);

		if (tmpDirName != null) {
			tmpDir = new File(tmpDirName);
		} else {
			tmpDir = new File(DEFAULT_TMP_DIR);
		}
	}

	@Override
	protected TransactionManager getTransactionManager() {
		return repository.getTransactionManager();
	}

	private void saveScript(ScriptUnit unit, String scriptBody) throws PersistenceException {
		repository.save(unit.scriptClassName, unit.scriptByteCode, scriptBody, unit.generatedSource);
	}

	private void saveScriptMetadata(ScriptUnit compilationUnit) throws PersistenceException {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put(Metadata.TYPE, Metadata.SCRIPT_TYPE);
		metaData.put(Metadata.NAME, compilationUnit.scriptName);
		metaData.put(Metadata.TITLE, TypeNameResolver.splitName(compilationUnit.scriptName).getChildName());

		writeInputParametersIfAny(metaData, compilationUnit);

		String packageName = ClassNameUtil.getPackageName(compilationUnit.scriptClassName);
		repository.savePackageMetadata(packageName, metaData);
	}

	private void writeInputParametersIfAny(Map<String, String> metaData, ScriptUnit compilationUnit)
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

	private void saveEntityMetadata(EntityUnit compilationUnit) throws PersistenceException {
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

	private ScriptUnit compileScript(String scriptName, String scriptBody) throws ManagementException {
		String scriptClassName = TypeNameResolver.resolveScriptClassName(scriptName);
		ScriptCompilationUnit compilationUnit;
		try {
			compilationUnit = ClassCompiler.compileScript(scriptClassName, scriptBody);
		} catch (ClassCompilationException e) {
			throw new ManagementException(String.format("Compilation of the script [%s] failed", scriptName), e);
		}

		ScriptUnit unit = new ScriptUnit();
		unit.scriptName = scriptName;
		unit.scriptParameters = compilationUnit.getInputParameters();
		unit.scriptClassName = scriptClassName;
		unit.scriptByteCode = compilationUnit.getByteCode();
		unit.generatedSource = compilationUnit.getGeneratedSource();

		return unit;
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

	private EntityUnit compileEntity(String entityName, String entityBody) throws ManagementException {
		String bodyJavaCode = transformToJavaCode(entityName, entityBody);
		String className = TypeNameResolver.resolveEntityClassName(entityName);

		byte[] byteCode;
		try {
			byteCode = ClassCompiler.compileEntity(className, bodyJavaCode);
		} catch (ClassCompilationException e) {
			throw new ManagementException(String.format("Compilation of the entity [%s] failed", entityName), e);
		}

		EntityUnit unit = new EntityUnit();
		unit.entityName = entityName;
		unit.entityClassName = className;
		unit.entityByteCode = byteCode;

		return unit;
	}

	private void saveEntity(EntityUnit compilationUnit, String entityBody) throws ManagementException {
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

	private Map<String, String> extractInputParameters(Map<String, String> meta, String scriptName)
			throws ManagementException {
		Map<String, String> result = EMPTY_MAP;

		String jsonParams = meta.get(Metadata.PARAMETERS);
		if (jsonParams != null) {
			Map<String, String> params;
			try {
				params = JsonUtil.toMap(jsonParams);
			} catch (IOException e) {
				throw new ManagementException(
						String.format("Failed to read \"input parameters\" meta-data for the script [%s]", scriptName),
						e);
			}
			result = params;
		}

		return result;
	}

	private File createTempJar(List<Entry<String, byte[]>> entityClasses) throws ManagementException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, TMP_ENTITIES_JAR_MANIFEST_VERSION);

		final String jarName = String.format(TMP_ENTITIES_JAR_NAME_TEMPLATE, System.currentTimeMillis());
		final File jarFile = new File(tmpDir, jarName);

		try {
			JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile), manifest);

			Set<String> processedPackages = new HashSet<>();
			for (Entry<String, byte[]> classEntry : entityClasses) {
				String className = classEntry.getKey();

				// Create dir within JAR, if it is not already there
				if (processedPackages.add(ClassNameUtil.getPackageName(className))) {
					// Make sure dir path contains UNIX slashes and ends with '/' (according to Jar spec)
					String classDir = ClassNameUtil.getDirs(className).replace(File.separatorChar, '/').concat("/");
					jar.putNextEntry(new JarEntry(classDir));
					jar.closeEntry();
				}

				// Create class within JAR
				JarEntry jarClass = new JarEntry(ClassNameUtil.getPath(className).replace(File.separatorChar, '/'));
				jar.putNextEntry(jarClass);
				byte[] byteCode = classEntry.getValue();
				IOUtils.copyLarge(new ByteArrayInputStream(byteCode), jar);
				jar.closeEntry();
			}

			jar.close();
		} catch (IOException e) {
			FileUtils.deleteQuietly(jarFile);
			throw new ManagementException("Failed to create entities library", e);
		}

		return jarFile;
	}

	private static class ScriptUnit {

		String scriptName;

		Map<String, String> scriptParameters;

		String scriptClassName;

		byte[] scriptByteCode;

		String generatedSource;
	}

	private static class EntityUnit {

		String entityName;

		String entityClassName;

		byte[] entityByteCode;
	}

}
