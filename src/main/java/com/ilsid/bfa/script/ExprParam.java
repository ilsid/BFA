package com.ilsid.bfa.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method parameter as a dynamic expression.
 * 
 * @author illia.sydorovych
 *
 */
@Target(value = ElementType.PARAMETER)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ExprParam {

	/**
	 * Defines whether an expression should be replaced before compilation.
	 * 
	 * @return <code>true</code> by default
	 */
	boolean replaceOnCompile() default true;

	/**
	 * Defines an expression type.
	 * 
	 * @return {@linkplain Type#ANY} by default.
	 */
	Type type() default Type.ANY;

	public static enum Type {
		ANY, VAR_NAME, VAR_OR_FLD_NAME;
	}
}
