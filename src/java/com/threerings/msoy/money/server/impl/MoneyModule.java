//
// $Id$

package com.threerings.msoy.money.server.impl;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

import com.google.inject.AbstractModule;
import com.threerings.msoy.money.server.MoneyLogic;

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
        bind(MoneyRepository.class).to(DepotMoneyRepository.class);
        bind(MoneyLogic.class).to(MoneyLogicImpl.class);
        bind(SecuredPricesCache.class).toInstance(new SecuredPricesEhcache(SECURED_PRICES_MAX_ELEMENTS,
            SECURED_PRICES_MAX_DURATION));
        bindInterceptor(any(), annotatedWith(Retry.class), new RetryInterceptor());
    }
    
    private static final int SECURED_PRICES_MAX_ELEMENTS = 100000;
    private static final int SECURED_PRICES_MAX_DURATION = 600;
}
