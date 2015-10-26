package com.ilsid.bfa.script;

/**
 * A compilation block. Contains a class name, a byte code and a source code of
 * a dynamic part (script body or dynamic expression).
 * 
 * @author illia.sydorovych
 *
 */
public class CompilationBlock {

	public CompilationBlock(String className, byte[] byteCode, String sourceCode) {
		this.className = className;
		this.byteCode = byteCode;
		this.sourceCode = sourceCode;
	}

	private String className;

	private byte[] byteCode;

	private String sourceCode;

	public String getClassName() {
		return className;
	}

	public byte[] getByteCode() {
		return byteCode;
	}

	public String getSourceCode() {
		return sourceCode;
	}

}
