package com.ilsid.bfa.compiler;

/**
 * Class compilation unit. Contains a class name, a byte code and a source code
 * of a dynamic part (script body or dynamic expression).
 * 
 * @author illia.sydorovych
 *
 */
public class CompilationUnit {

	private String className;

	private byte[] byteCode;

	private String sourceCode;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public byte[] getByteCode() {
		return byteCode;
	}

	public void setByteCode(byte[] byteCode) {
		this.byteCode = byteCode;
	}

	public String getSourceCode() {
		return sourceCode;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

}
