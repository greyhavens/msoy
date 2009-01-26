//
// $Id$

package com.threerings.msoy.server.persist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

import com.samskivert.util.Invoker;

/**
 * An annotation that identifies an {@link Invoker} specifically designated for batch jobs that
 * may take a while to execute and which would hold up the main invoker for unacceptably long
 * periods of time. Code that requires the ability to post units for execution on the batch
 * invoker thread can inject this queue like so:
 *
 * <code>@Inject @BatchInvoker Invoker _invoker;</code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@BindingAnnotation
public @interface BatchInvoker
{
}
