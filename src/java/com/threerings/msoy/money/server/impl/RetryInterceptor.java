//
// $Id$

package com.threerings.msoy.money.server.impl;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;

/**
 * Interceptor that will retry a method call if a certain exception occurs.  The
 * method must be annotated with {@link Retry}, specifying the exception that
 * will cause the method to be retried.  If the method has been attempted the
 * maximum number of times, a runtime exception will be thrown.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class RetryInterceptor
    implements MethodInterceptor
{
    public Object invoke (final MethodInvocation invocation)
        throws Throwable
    {
        // The required annotation contains the exception on which we should retry
        // and the max times to retry.
        final Retry retryAnn = invocation.getMethod().getAnnotation(Retry.class);
        
        int retries = 0;
        Throwable lastException = null;
        do {
            try {
                return invocation.proceed();
            } catch (final Throwable t) {
                if (retryAnn.exception().isInstance(t)) {
                    retries++;
                    logger.info("Retrying method: " + invocation.getMethod().toString() + 
                        ", exception: " + t.getMessage() + ", attempts: " + retries);
                    lastException = t;
                } else {
                    throw t;
                }
            }
        } while (retries < retryAnn.attempts());
        
        throw new IllegalStateException("Cannot continue retrying method:" + 
            invocation.getMethod().toString(), lastException);
    }

    private static final Logger logger = Logger.getLogger(RetryInterceptor.class);
}
