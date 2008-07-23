//
// $Id$

package com.threerings.msoy.money.server;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.threerings.msoy.money.server.impl.MoneyModule;
import com.threerings.msoy.server.ServerConfig;

/**
 * This really should be an integration test that's executed on every build, but it's not
 * current setup to handle database updates (it will try modifying the database, there's
 * no way to rollback, etc.).
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyTest
{
    @Test
    public void testBuyBars ()
    {
        final MemberMoney oldMoney = service.getMoneyFor(1);
        service.buyBars(1, 2);
        final MemberMoney newMoney = service.getMoneyFor(1);
        assertEquals(oldMoney.getBars() + 2, newMoney.getBars());
    }
    
    @Before
    public void setup () throws Exception
    {
        final ConnectionProvider connProv = ServerConfig.createConnectionProvider();
        final Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure ()
            {
                bind(PersistenceContext.class).toInstance(new PersistenceContext("msoy", connProv, null));
                install(new MoneyModule());
            }
        });
        injector.injectMembers(this);
    }
    
    @Inject private MoneyService service;
}
