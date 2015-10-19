package com.ilsid.bfa.compiler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a method sets a variable.
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
