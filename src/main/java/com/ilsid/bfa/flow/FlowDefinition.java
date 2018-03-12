package com.ilsid.bfa.flow;

import java.util.List;

public class FlowDefinition {
	
	private List<String> inputParameters;
	
	private List<String> localVariables;
	
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


	public static class Block {
		
		private String id;
		
		private String type;
		
		private String name;
		
		private String description;
		
		private String output;
		
		private List<String> inputParameters;
		
		private List<String> preExecExpressions;
		
		private List<String> postExecExpressions;
		
		private List<String> expressions;


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

	}
	
}
