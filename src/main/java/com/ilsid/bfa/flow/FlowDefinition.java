package com.ilsid.bfa.flow;

import java.util.LinkedList;
import java.util.List;

/**
 * Flow Definition representation.
 * 
 * @author illia.sydorovych
 *
 */
public class FlowDefinition {

	private List<String> inputParameters = new LinkedList<>();

	private List<String> localVariables = new LinkedList<>();

	private List<Block> blocks;

	public List<String> getInputParameters() {
		return inputParameters;
	}

	public void setInputParameters(List<String> inputParameters) {
		this.inputParameters = inputParameters;
	}

	public List<String> getLocalVariables() {
		return localVariables;
	}

	public void setLocalVariables(List<String> localVariables) {
		this.localVariables = localVariables;
	}

	public List<Block> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}

	/**
	 * Flow Block: Start, End, Action, Sub-Process, Condition.
	 * 
	 * @author illia.sydorovych
	 *
	 */
	public static class Block {

		private String id;

		private String type;

		private String name;

		private String description;

		private String output;

		private List<String> inputParameters = new LinkedList<>();

		private List<String> preExecExpressions = new LinkedList<>();

		private List<String> postExecExpressions = new LinkedList<>();

		private List<String> expressions = new LinkedList<>();
		
		private List<Block> trueBranch;
		
		private List<Block> falseBranch;


		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getOutput() {
			return output;
		}

		public void setOutput(String output) {
			this.output = output;
		}

		public List<String> getInputParameters() {
			return inputParameters;
		}

		public void setInputParameters(List<String> inputParameters) {
			this.inputParameters = inputParameters;
		}

		public List<String> getPreExecExpressions() {
			return preExecExpressions;
		}

		public void setPreExecExpressions(List<String> preExecExpressions) {
			this.preExecExpressions = preExecExpressions;
		}

		public List<String> getPostExecExpressions() {
			return postExecExpressions;
		}

		public void setPostExecExpressions(List<String> postExecExpressions) {
			this.postExecExpressions = postExecExpressions;
		}

		public List<String> getExpressions() {
			return expressions;
		}

		public void setExpressions(List<String> expressions) {
			this.expressions = expressions;
		}
		
		public List<Block> getTrueBranch() {
			return trueBranch;
		}

		public void setTrueBranch(List<Block> trueBranch) {
			this.trueBranch = trueBranch;
		}

		public List<Block> getFalseBranch() {
			return falseBranch;
		}

		public void setFalseBranch(List<Block> falseBranch) {
			this.falseBranch = falseBranch;
		}
		

	}
	
	public static class BlockType {

		public static final String START = "start";

		public static final String END = "end";

		public static final String ACTION = "action";

		public static final String SUBPROCESS = "subprocess";

		public static final String CONDITION = "condition";

	}

}
