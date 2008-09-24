//
// $Id$

package com.threerings.msoy.server.util;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.samskivert.util.Logger;

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
        Retry retryAnn = null;
        int retries = 0;
        do {
            try {
                return invocation.proceed();
            } catch (final Throwable t) {
                if (retryAnn == null) {
                    retryAnn = invocation.getMethod().getAnnotation(Retry.class);
                }
                if (!retryAnn.exception().isInstance(t)) {
                    throw t; // not our exception, just throw it
                }
                retries++;
                if (retries < retryAnn.attempts()) {
                    _log.info("Retrying method.",
                        "method", invocation.getMethod(),
                        "attempts", retries,
                        "exception", t.getMessage());
                } else {
                    throw new IllegalStateException("Cannot continue retrying method:" + 
                        invocation.getMethod().toString(), t);
                }
            }
        } while (true);
    }

    private static final Logger _log = Logger.getLogger(RetryInterceptor.class);
}
