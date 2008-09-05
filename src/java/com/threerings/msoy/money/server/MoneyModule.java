//
// $Id$

package com.threerings.msoy.money.server;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

import com.google.inject.AbstractModule;
import com.threerings.msoy.money.server.impl.Retry;
import com.threerings.msoy.money.server.impl.RetryInterceptor;

/**
 * Dependency injection module for the money service.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public final class MoneyModule extends AbstractModule
{
    @Override
    protected void configure ()
    {
        bindInterceptor(any(), annotatedWith(Retry.class), new RetryInterceptor());
    }
}
