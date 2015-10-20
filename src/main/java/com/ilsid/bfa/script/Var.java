package com.ilsid.bfa.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * States that a method declares a variable. The first method argument contains
 * a variable name. The second argument contains a string representation of
 * variable type. Both arguments are string literals.
 * 
 * @author illia.sydorovych
 *
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Var {

	Scope scope();

	public static enum Scope {
		INPUT, LOCAL
	}
}
