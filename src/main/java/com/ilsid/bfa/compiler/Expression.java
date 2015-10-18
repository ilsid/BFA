package com.ilsid.bfa.compiler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method parameter as expression. 
 * 
 * @author illia.sydorovych
 *
 */
@Target(value=ElementType.PARAMETER)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Expression {

}