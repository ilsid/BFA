package com.ilsid.bfa.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.io.IOUtils;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.TransactionManager;
import com.ilsid.bfa.script.ClassCompilationException;
import com.ilsid.bfa.script.ClassCompiler;
import com.ilsid.bfa.script.CompilationBlock;
import com.ilsid.bfa.script.CompilerConstants;

//TODO: write javadocs
public class ScriptManager {

	private CodeRepository repository;

	public void createScript(String scriptName, InputStream scriptBody) throws ManagementException {
		String scriptClassName = ClassNameUtil.resolveScriptClassName(scriptName);
		try {
			byte[] scriptByteCode = ClassCompiler.compileScriptToBytecode(scriptClassName, scriptBody);
			String scriptBodySource = IOUtils.toString(scriptBody, "UTF-8");

			Collection<CompilationBlock> expressions;
			try (InputStream scriptSourceCode = IOUtils.toInputStream(
					String.format(CompilerConstants.SCRIPT_SOURCE_TEMPLATE, scriptClassName, scriptBodySource));) {
				String scriptShortClassName = ClassNameUtil.resolveShortClassName(scriptClassName);
				expressions = ClassCompiler.compileScriptExpressions(scriptShortClassName, scriptSourceCode);
			}

			startTransaction();
			repository.save(scriptClassName, scriptByteCode, scriptBodySource);

			for (CompilationBlock expr : expressions) {
				repository.save(expr.getClassName(), expr.getByteCode(), expr.getSourceCode());
			}
			commitTransaction();

		} catch (ClassCompilationException | IOException e) {
			rollbackTransaction();
			throw new ManagementException(String.format("Compilation of the script [%] failed", scriptName), e);
		} catch (PersistenceException e) {
			rollbackTransaction();
			throw new ManagementException(String.format("Failed to persist the script [%]", scriptName), e);
		}
	}

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
