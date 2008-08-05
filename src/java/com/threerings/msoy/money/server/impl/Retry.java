//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method annotation that indicates it should be retried up to some number of times if
 * an exception is thrown when calling the method.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Retry 
{
    /** The exception that indicates when the method should be retried. */
    Class<? extends Exception> exception();
    
    /** Maximum number of attempts to run the method.  Defaults to 3. */
    int attempts() default 3;
}
