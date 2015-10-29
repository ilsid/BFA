package com.ilsid.bfa.common;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.ilsid.bfa.script.CompilerConstants;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;

public class CompileHelper {
	
	private static final String GENERATED_PACKAGE = "com.ilsid.bfa.generated.";

	public static final String GENERATED_SCRIPT_PACKAGE = GENERATED_PACKAGE + "script.";
	
	private static final CtClass[] NO_ARGS = {};

	public static byte[] compileScript(String className, InputStream scriptBody) throws Exception {
		CtClass clazz = buildScriptClass(className, scriptBody);
		byte[] result = toBytecode(clazz);

		return result;
	}

	private static CtClass buildScriptClass(String className, InputStream scriptBody)
			throws Exception {
		ClassPool classPool = ClassPool.getDefault();
		CtClass clazz = classPool.makeClass(className);
		clazz.setSuperclass(classPool.get(CompilerConstants.SCRIPT_CLASS_NAME));

		CtConstructor cons = new CtConstructor(NO_ARGS, clazz);
		cons.setBody(";");
		clazz.addConstructor(cons);

		CtMethod method = new CtMethod(CtClass.voidType, "doExecute", NO_ARGS, clazz);
		method.setModifiers(Modifier.PROTECTED);
		String body = "{" + StringUtils.LF + IOUtils.toString(scriptBody, "UTF-8") + StringUtils.LF + "}";
		method.setBody(body);
		clazz.addMethod(method);

		return clazz;
	}
	
	private static byte[] toBytecode(CtClass clazz) throws Exception {
		byte[] result = clazz.toBytecode();
		clazz.detach();
		return result;
	}
	
}
