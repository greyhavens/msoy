//
// $Id$

package com.threerings.msoy.money.server.impl;

import com.google.inject.AbstractModule;
import com.threerings.msoy.money.server.MoneyService;

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
        bind(MoneyService.class).to(MoneyServiceImpl.class);
    }
}
