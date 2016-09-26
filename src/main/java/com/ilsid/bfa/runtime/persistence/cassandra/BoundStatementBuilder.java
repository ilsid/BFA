package com.ilsid.bfa.runtime.persistence.cassandra;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;

/**
 * Builder for bound statements.
 * 
 * @author illia.sydorovych
 *
 */
public class BoundStatementBuilder {

	private PreparedStatement statement;
	
	private ScriptRuntimeCriteria criteria;

	BoundStatementBuilder() {
	}

	public static BoundStatementBuilder newInstance() {
		return new BoundStatementBuilder();
	}

	public BoundStatementBuilder forStatement(PreparedStatement statement) {
		this.statement = statement;
		return this;
	}
	
	public BoundStatementBuilder withCriteria(ScriptRuntimeCriteria criteria) {
		this.criteria = criteria;
		return this;
	}
	
	public BoundStatement build() {
		final BoundStatement result = new BoundStatement(statement);
		return result;
	}
}
