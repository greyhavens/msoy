//
// $Id$

package com.threerings.msoy.money.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
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
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = service.getMoneyFor(1);
        service.buyBars(1, 2);
        final MemberMoney newMoney = service.getMoneyFor(1);
        assertEquals(oldMoney.getBars() + 2, newMoney.getBars());
        assertEquals(oldMoney.getAccBars() + 2, newMoney.getAccBars());
        final long endTime = System.currentTimeMillis() / 1000;
        
        final List<MoneyHistory> log = service.getLog(1, null, 0, 30, true);
        checkMoneyHistory(log, new MoneyHistory(1, new Date(), MoneyType.BARS, 2.0, false, 
            "Purchased 2 bars.", null), startTime, endTime);
    }
    
    @Test
    public void testBuyBarItemWithBars ()
        throws Exception
    {
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = service.getMoneyFor(1);
        service.buyBars(1, 150);
        final ItemIdent item = new ItemIdent(Item.AVATAR, 1);
        service.secureBarPrice(1, 2, 3, item, 100, "My bar item");
        service.buyItemWithBars(1, item);
        final MemberMoney newMoney = service.getMoneyFor(1);
        assertEquals(oldMoney.getBars() + 50, newMoney.getBars());
        assertEquals(oldMoney.getAccBars() + 150, newMoney.getAccBars());
        final long endTime = System.currentTimeMillis() / 1000;

        final List<MoneyHistory> log = service.getLog(1, null, 0, 30, true);
        checkMoneyHistory(log, new MoneyHistory(1, new Date(), MoneyType.BARS, 100.0, true, 
            "My bar item", item), startTime, endTime);
        
        // TODO: check creator and affiliate
    }
    
    @Test(expected=NotEnoughMoneyException.class)
    public void testNotEnoughBars ()
        throws Exception
    {
        final MemberMoney oldMoney = service.getMoneyFor(1);
        final ItemIdent item = new ItemIdent(Item.AVATAR, 1);
        service.secureBarPrice(1, 2, 3, item, oldMoney.getBars() + 1, "My bar item");
        service.buyItemWithBars(1, item);
    }
        
    @Test(expected=NotEnoughMoneyException.class)
    public void testNotEnoughCoins ()
        throws Exception
    {
        final MemberMoney oldMoney = service.getMoneyFor(1);
        final ItemIdent item = new ItemIdent(Item.AVATAR, 1);
        service.secureCoinPrice(1, 2, 3, item, oldMoney.getCoins() + 1, "My coin item");
        service.buyItemWithCoins(1, item);
    }
    
    @Test(expected=NotSecuredException.class)
    public void testNotSecured ()
        throws Exception
    {
        final ItemIdent item = new ItemIdent(Item.AVATAR, 1);
        service.buyItemWithBars(1, item);
    }
    
    @Test
    public void testBuyCoinItemWithCoins ()
        throws Exception
    {
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = service.getMoneyFor(1);
        service.awardCoins(1, 2, 3, 150);
        final ItemIdent item = new ItemIdent(Item.AVATAR, 1);
        service.secureCoinPrice(1, 2, 3, item, 100, "testBuyCoinItemWithCoins - test");
        service.buyItemWithCoins(1, item);
        final MemberMoney newMoney = service.getMoneyFor(1);
        assertEquals(oldMoney.getCoins() + 50, newMoney.getCoins());
        assertEquals(oldMoney.getAccCoins() + 150, newMoney.getAccCoins());
        final long endTime = System.currentTimeMillis() / 1000;
        
        final List<MoneyHistory> log = service.getLog(1, null, 0, 30, true);
        checkMoneyHistory(log, new MoneyHistory(1, new Date(), MoneyType.COINS, 100.0, true, 
            "testBuyCoinItemWithCoins - test", item), startTime, endTime);
        
        // TODO: check creator and affiliate
        
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
    
    private void checkMoneyHistory (final List<MoneyHistory> log, final MoneyHistory expected, 
        final long start, final long end)
    {
        MoneyHistory logEntry = null;
        for (final MoneyHistory history : log) {
            if (history.getDescription().equals(expected.getDescription())) {
                logEntry = history;
                break;
            }
        }
        if (logEntry == null) {
            fail("No appropriate log entry found.");
        }
        assertEquals(expected.getAmount(), logEntry.getAmount(), 0.0);
        assertEquals(expected.getItem(), logEntry.getItem());
        assertEquals(expected.getMemberId(), logEntry.getMemberId());
        assertEquals(expected.getType(), logEntry.getType());
        final long time = logEntry.getTimestamp().getTime() / 1000;
        assertTrue(time >= start && time <= end);
        assertEquals(expected.isSpent(), logEntry.isSpent());
    }
    
    @Inject private MoneyLogic service;
}
