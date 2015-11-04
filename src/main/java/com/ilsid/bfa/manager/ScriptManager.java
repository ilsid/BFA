package com.ilsid.bfa.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.TransactionManager;
import com.ilsid.bfa.script.ClassCompilationException;
import com.ilsid.bfa.script.ClassCompiler;
import com.ilsid.bfa.script.CompilationBlock;
import com.ilsid.bfa.script.CompilerConstants;

/**
 * Provides a set of management operations for scripts.
 * 
 * @author illia.sydorovych
 *
 */
// TODO: write unit tests
public class ScriptManager {

	private CodeRepository repository;

	/**
	 * Creates new script in the repository.
	 * 
	 * @param scriptName
	 *            script name
	 * @param scriptBody
	 *            script body
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if the script itself or any of its expressions can't be
	 *             compiled or persisted</li>
	 *             <li>if the script with such name already exist in the
	 *             repository</li>
	 *             </ul>
	 */
	public void createScript(String scriptName, String scriptBody) throws ManagementException {
		String scriptClassName = ClassNameUtil.resolveScriptClassName(scriptName);
		try {
			byte[] scriptByteCode = ClassCompiler.compileScriptToBytecode(scriptClassName, scriptBody);
			
			Collection<CompilationBlock> expressions;
			String scriptShortClassName = ClassNameUtil.getShortClassName(scriptClassName);
			try (InputStream scriptSourceCode = IOUtils.toInputStream(
					String.format(CompilerConstants.SCRIPT_SOURCE_TEMPLATE, scriptShortClassName, scriptBody));) {
				expressions = ClassCompiler.compileScriptExpressions(scriptSourceCode);
			}

			startTransaction();
			repository.save(scriptClassName, scriptByteCode, scriptBody);

			for (CompilationBlock expr : expressions) {
				repository.save(expr.getClassName(), expr.getByteCode());
			}
			commitTransaction();

		} catch (ClassCompilationException | IOException e) {
			rollbackTransaction();
			throw new ManagementException(String.format("Compilation of the script [%s] failed", scriptName), e);
		} catch (PersistenceException e) {
			rollbackTransaction();
			throw new ManagementException(String.format("Failed to persist the script [%s]", scriptName), e);
		}
	}

	/**
	 * Defines a repository implementation.
	 * 
	 * @param repository
	 *            a code repository
	 */
	@Inject
	public void setRepository(CodeRepository repository) {
		this.repository = repository;
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

}
