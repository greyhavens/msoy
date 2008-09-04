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
        bind(MoneyConfiguration.class).toInstance(
            new MoneyConfiguration(CREATOR_PERCENTAGE, AFFILIATE_PERCENTAGE, BLING_CASH_OUT,
                SECURED_PRICES_MAX_DURATION, SECURED_PRICES_MAX_ELEMENTS));
        bindInterceptor(any(), annotatedWith(Retry.class), new RetryInterceptor());
    }

    // TODO: these move. Maybe MoneyConfiguration becomes a server config object..
    private static final float CREATOR_PERCENTAGE = .3f;
    private static final float AFFILIATE_PERCENTAGE = .3f;
    private static final float BLING_CASH_OUT = 0f;
    private static final int SECURED_PRICES_MAX_ELEMENTS = 100000;
    private static final int SECURED_PRICES_MAX_DURATION = 10;
}
