package com.ilsid.bfa.flow;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * States that a method represents a flow element.
 * 
 * @author illia.sydorovych
 *
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface FlowElement {

	String type();

	String description() default "";

}
