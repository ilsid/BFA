package com.ilsid.bfa.generation;

import java.net.URL;
import java.net.URLClassLoader;

import org.jmock.integration.junit4.JMock;
import org.junit.Ignore;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;

public class GenerationUnitTest extends BaseUnitTestCase {
	
	private static final CtClass[] NO_ARGS = {};
	
	@Test
	@Ignore
	public void generateClassWithInterface() throws Exception {
		String className = "com.ilsid.bfa.test.generated.SomeAction$ChargeConditionCheckImpl";

		ClassPool pool = ClassPool.getDefault();
		CtClass clazz = pool.makeClass(className);
		clazz.addInterface(pool.get(SomeAction.class.getName()));
		
		CtClass scriptContextClass = pool.get(ScriptContextSimulator.class.getName());
		CtField field = new CtField(scriptContextClass, "scriptContext", clazz);
		field.setModifiers(Modifier.PRIVATE);
		clazz.addField(field);
		
		CtConstructor cons = new CtConstructor(NO_ARGS, clazz);
	    cons.setBody(";");
	    clazz.addConstructor(cons);
	    
	    CtMethod method;
		method = new CtMethod(CtClass.voidType, "setContext", new CtClass[] { scriptContextClass }, clazz);
		method.setBody("scriptContext = $1;");
		clazz.addMethod(method);
		
		method = new CtMethod(pool.get(Object.class.getName()), "doIt", NO_ARGS, clazz);
		method.setBody("return Double.valueOf(((com.ilsid.bfa.test.types.Subscriber) scriptContext.getLocalVar(\"Subscriber\")).PrepaidAmount.doubleValue() - ((com.ilsid.bfa.test.types.Subscriber) scriptContext.getLocalVar(\"Subscriber\")).PrepaidReserved.doubleValue());");
		clazz.addMethod(method);
		
		byte[] bytes = clazz.toBytecode();
		pool.appendClassPath(new ByteArrayClassPath(className, bytes));
		clazz = pool.get(className);
		
		ScriptContextSimulator scriptContext = new ScriptContextSimulator();
		
		//SomeAction action = (SomeAction) clazz.toClass(this.getClass().getClassLoader(), null).newInstance();
		SomeAction action = (SomeAction) clazz.toClass().newInstance();
		action.setContext(scriptContext);
		System.out.println("scriptContext: " + scriptContext);
		
		assertEquals(ScriptContextSimulator.class.getClassLoader(), action.getClass().getClassLoader());
		
		System.out.println("ScriptContextSimulator.class.getClassLoader(): " + ScriptContextSimulator.class.getClassLoader());
		System.out.println("action.getClass().getClassLoader(): " + action.getClass().getClassLoader());
		System.out.println(action.getClass().getName());
		System.out.println("Action result: " + action.doIt());
		
		System.out.println("Instanse of URLClassLoader?: " + (this.getClass().getClassLoader() instanceof URLClassLoader));
		URL[] urls = ((URLClassLoader) this.getClass().getClassLoader()).getURLs();
		for (int i = 0; i < urls.length; i++) {
            System.out.println(" " + urls[i]);
        }
	}
	
	private class SomeActionImpl implements SomeAction {
		
		private ScriptContextSimulator scriptContext;

		public Object doIt() {
			return ((com.ilsid.bfa.test.types.Subscriber) scriptContext.getLocalVar("Subscriber")).PrepaidAmount - ((com.ilsid.bfa.test.types.Subscriber) scriptContext.getLocalVar("Subscriber")).PrepaidReserved;
		}

		public void setContext(ScriptContextSimulator scriptContext) {
			this.scriptContext = scriptContext;
			
		}
		
	}
			
	
}
