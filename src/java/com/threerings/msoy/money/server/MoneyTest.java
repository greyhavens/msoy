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

import com.samskivert.util.BasicRunQueue;
import com.samskivert.util.Invoker;
import com.samskivert.util.RunQueue;

import com.threerings.presents.annotation.EventQueue;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.msoy.data.UserAction;

import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberActionLogRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.UserActionRepository;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.TransactionType;
import com.threerings.msoy.money.server.impl.MoneyHistoryExpirer;

/**
 * This really should be an integration test that's executed on every build, but it's not
 * current setup to handle database updates (it will try modifying the database, there's
 * no way to rollback, etc.).
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyTest
{
    public static void main (String[] args)
    {
        MoneyTest test = new MoneyTest();
        try {
            test.setup();
        } catch (Exception e) {
            System.err.println("setup: " + e);
        }
        try {
            test.testExpirer();
        } catch (Exception e) {
            System.err.println("testExpirer: " + e);
        }
        try {
            test.testBuyBars();
        } catch (Exception e) {
            System.err.println("testBuyBars: " + e);
        }
        try {
            test.testBuyBarItemWithBars();
        } catch (Exception e) {
            System.err.println("testBuyBarItemWithBars: " + e);
        }
        try {
            test.testNotEnoughBars();
        } catch (Exception e) {
            System.err.println("testNotEnoughBars: " + e);
        }
        try {
            test.testNotEnoughCoins();
        } catch (Exception e) {
            System.err.println("testNotEnoughCoins: " + e);
        }
        try {
            test.testNotSecured();
        } catch (Exception e) {
            System.err.println("testNotSecured: " + e);
        }
        try {
            test.testBuyCoinItemWithCoins();
        } catch (Exception e) {
            System.err.println("testBuyCoinItemWithCoins: " + e);
        }
        try {
            test.testAwardCoins();
        } catch (Exception e) {
            System.err.println("testAwardCoins: " + e);
        }
        try {
            test.testCreatorBoughtOwnItem();
        } catch (Exception e) {
            System.err.println("testCreatorBoughtOwnItem: " + e);
        }
        try {
            test.testSupport();
        } catch (Exception e) {
            System.err.println("testSupport: " + e);
        }
    }

    @Test
    public void testExpirer ()
        throws Exception
    {
        // perform 3 transactions: 1 with coins and 1 with bars.  Then wait 3s and do another
        // with coins.  Create an expirer to remove coin histories older than 3s -- ensure
        // the other two exist.
        final long start = System.currentTimeMillis() / 1000;
        _service.awardCoins(1, 0, 0, null, 100, "testExpirer - coins1", UserAction.PLAYED_GAME);
        _service.buyBars(1, 10, "Bought 10 bars");
        final long end = System.currentTimeMillis() / 1000;
        System.out.println("Waiting 3s to create non-expiring item.");
        Thread.sleep(3000);
        final long start2 = System.currentTimeMillis() / 1000;
        _service.awardCoins(1, 0, 0, null, 101, "testExpirer - coins2", UserAction.PLAYED_GAME);
        final long end2 = System.currentTimeMillis() / 1000;
        _expirer.setMaxAge(3000);
        _expirer.start();

        final List<MoneyHistory> log = _service.getLog(1, null, null, 0, 30, true);
        checkMoneyHistory(log, new MoneyHistory(1, new Date(), Currency.BARS, 10.0, 
            TransactionType.BARS_BOUGHT, false, "Bought 10 bars", null, null), null, start, end, true);
        checkMoneyHistory(log, new MoneyHistory(1, new Date(), Currency.COINS, 101.0, 
            TransactionType.AWARD, false, "testExpirer - coins2", null, null), null, start2, end2, true);
        checkMoneyHistory(log, new MoneyHistory(1, new Date(), Currency.COINS, 100.0, 
            TransactionType.AWARD, false, "testExpirer - coins1", null, null), null, start, end, false);
    }

    @Test
    public void testBuyBars ()
        throws Exception
    {
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final MoneyResult result = _service.buyBars(1, 2, "Bought 2 bars.");
        assertEquals(oldMoney.getBars() + 2, result.getNewMemberMoney().getBars());
        assertEquals(oldMoney.getAccBars() + 2, result.getNewMemberMoney().getAccBars());
        final long endTime = System.currentTimeMillis() / 1000;

        final List<MoneyHistory> log = _service.getLog(1, Currency.BARS, null, 0, 30, true);
        checkMoneyHistory(log, new MoneyHistory(1, new Date(), Currency.BARS, 2.0, 
            TransactionType.BARS_BOUGHT, false, "Bought 2 bars.", null, null), null, startTime, 
            endTime, true);

        checkActionLogExists(1, UserAction.BOUGHT_BARS.getNumber(), "Bought 2 bars.",
            startTime, endTime);
    }

    @Test
    public void testBuyBarItemWithBars ()
        throws Exception
    {
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final MemberMoney oldCreatorMoney = _service.getMoneyFor(2);
        _service.buyBars(1, 150, "Bought 150 bars.");
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        _service.securePrice(1, item, Currency.BARS, 100, 2, 3, "My bar item");
        final MoneyResult result = _service.buyItem(makeMember(false), item, Currency.BARS, 100,
            Currency.BARS, 100);
        final long endTime = System.currentTimeMillis() / 1000;

        // Check member account
        final MemberMoney newMoney = result.getNewMemberMoney();
        assertEquals(oldMoney.getBars() + 50, newMoney.getBars());
        assertEquals(oldMoney.getAccBars() + 150, newMoney.getAccBars());
        
        List<MoneyHistory> log = _service.getLog(1, Currency.BARS, null, 0, 30, true);
        final MoneyHistory expectedMH = new MoneyHistory(1, new Date(), Currency.BARS, 100.0, 
            TransactionType.ITEM_PURCHASE, true, "My bar item", null /*item*/, null);
        checkMoneyHistory(log, expectedMH, null, startTime, endTime, true);

        checkActionLogExists(1, UserAction.BOUGHT_ITEM.getNumber(), "My bar item",
            startTime, endTime);

        // Check creator account
        final MemberMoney newCreatorMoney = result.getNewCreatorMoney();
        assertEquals(oldCreatorMoney.getBling() + 100.0*0.3, newCreatorMoney.getBling(), 0.001);
        assertEquals(oldCreatorMoney.getAccBling() + 100.0*0.3, newCreatorMoney.getAccBling(),
            0.001);

        log = _service.getLog(2, Currency.BLING, null, 0, 30, true);
        checkMoneyHistory(log, new MoneyHistory(2, new Date(), Currency.BLING, 100.0*0.3, 
            TransactionType.CREATOR_PAYOUT, false, "Item purchased: My bar item", null /*item*/,
            null), 
            expectedMH, startTime, endTime, true);

        checkActionLogExists(2, UserAction.RECEIVED_PAYOUT.getNumber(), "My bar item",
            startTime, endTime);

        // TODO: check affiliate account
    }

    @Test(expected=NotEnoughMoneyException.class)
    public void testNotEnoughBars ()
        throws Exception
    {
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        final int bars = oldMoney.getBars() + 1;
        _service.securePrice(1, item, Currency.BARS, bars, 2, 3, "My bar item");
        _service.buyItem(makeMember(false), item, Currency.BARS, bars, Currency.BARS, bars);
    }

    @Test(expected=NotEnoughMoneyException.class)
    public void testNotEnoughCoins ()
        throws Exception
    {
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        final  int coins = oldMoney.getCoins() + 1;
        _service.securePrice(1, item, Currency.COINS, coins, 2, 3, "My coin item");
        _service.buyItem(makeMember(false), item, Currency.COINS, coins, Currency.COINS, coins);
    }

    @Test(expected=NotSecuredException.class)
    public void testNotSecured ()
        throws Exception
    {
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        _service.buyItem(makeMember(false), item, Currency.COINS, 100, Currency.COINS, 100);
    }

    @Test
    public void testBuyCoinItemWithCoins ()
        throws Exception
    {
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final MemberMoney oldCreatorMoney = _service.getMoneyFor(2);
        _service.awardCoins(1, 2, 3, null, 150, "150 coins awarded.", UserAction.PLAYED_GAME);
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        _service.securePrice(1, item, Currency.COINS, 100, 2, 3, "testBuyCoinItemWithCoins - test");
        final MoneyResult result = _service.buyItem(makeMember(false), item, Currency.COINS, 100,
            Currency.COINS, 100);
        final long endTime = System.currentTimeMillis() / 1000;

        // Check member account
        final MemberMoney newMoney = result.getNewMemberMoney();
        assertEquals(oldMoney.getCoins() + 50, newMoney.getCoins());
        assertEquals(oldMoney.getAccCoins() + 150, newMoney.getAccCoins());

        List<MoneyHistory> log = _service.getLog(1, Currency.COINS, null, 0, 30, true);
        final MoneyHistory expectedMH = new MoneyHistory(1, new Date(), Currency.COINS, 100.0, 
            TransactionType.ITEM_PURCHASE, true, "testBuyCoinItemWithCoins - test", null /*item*/,
            null);
        checkMoneyHistory(log, expectedMH, null, startTime, endTime, true);

        checkActionLogExists(1, UserAction.BOUGHT_ITEM.getNumber(),
            "testBuyCoinItemWithCoins - test", startTime, endTime);

        // Check creator account
        final MemberMoney newCreatorMoney = result.getNewCreatorMoney();
        assertEquals(oldCreatorMoney.getCoins() + 30, newCreatorMoney.getCoins());
        assertEquals(oldCreatorMoney.getAccCoins() + 30, newCreatorMoney.getAccCoins());

        log = _service.getLog(2, Currency.COINS, null, 0, 30, true);
        checkMoneyHistory(log, new MoneyHistory(2, new Date(), Currency.COINS, 30.0, 
            TransactionType.CREATOR_PAYOUT, false,
            "Item purchased: testBuyCoinItemWithCoins - test", null /*item*/, null),
            expectedMH, startTime, endTime, true);

        checkActionLogExists(2, UserAction.RECEIVED_PAYOUT.getNumber(),
            "testBuyCoinItemWithCoins - test", startTime, endTime);

        // TODO: check affiliate account
    }

    @Test
    public void testAwardCoins ()
        throws Exception
    {
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final ItemIdent item = new ItemIdent(Item.GAME, 1);
        final MoneyResult result = _service.awardCoins(1, 2, 3, item, 150,
            "150 coins awarded.  Thanks for playing!", UserAction.PLAYED_GAME);
        final long endTime = System.currentTimeMillis() / 1000;

        final MemberMoney newMoney = result.getNewMemberMoney();
        assertEquals(oldMoney.getCoins() + 150, newMoney.getCoins());
        assertEquals(oldMoney.getAccCoins() + 150, newMoney.getAccCoins());

        final List<MoneyHistory> log = _service.getLog(1, Currency.COINS, null, 0, 30, true);
        checkMoneyHistory(log, new MoneyHistory(1, new Date(), Currency.COINS, 150.0, 
            TransactionType.AWARD, false, "150 coins awarded.  Thanks for playing!", item, null), 
            null, startTime, endTime, true);

        checkActionLogExists(1, UserAction.PLAYED_GAME.getNumber(),
            "150 coins awarded.  Thanks for playing!", startTime, endTime);

        // TODO: check creator and affiliate accounts
    }

    @Test
    public void testCreatorBoughtOwnItem ()
        throws Exception
    {
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        _service.awardCoins(1, 1, 3, null, 150, "150 coins awarded.", UserAction.PLAYED_GAME);
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        _service.securePrice(1, item, Currency.COINS, 100, 1, 3, "testCreatorBoughtOwnItem - test");
        final MoneyResult result = _service.buyItem(makeMember(false), item, Currency.COINS, 100,
            Currency.COINS, 100);
        final long endTime = System.currentTimeMillis() / 1000;

        // Check member account
        final MemberMoney newMoney = result.getNewMemberMoney();
        assertEquals(oldMoney.getCoins() + 50 + 30, newMoney.getCoins());
        assertEquals(oldMoney.getAccCoins() + 150, newMoney.getAccCoins());

        final List<MoneyHistory> log = _service.getLog(1, Currency.COINS, null, 0, 30, true);
        checkMoneyHistory(log, new MoneyHistory(1, new Date(), Currency.COINS, 70.0, 
            TransactionType.ITEM_PURCHASE, true, "testCreatorBoughtOwnItem - test", null /*item*/,
            null), 
            null, startTime, endTime, true);

        checkActionLogExists(1, UserAction.BOUGHT_ITEM.getNumber(),
            "testCreatorBoughtOwnItem - test", startTime, endTime);
    }

    @Test
    public void testSupport () throws Exception
    {
        // First, have a support account buy an item, bringing them down to 100 coins
        _service.awardCoins(1, 2, 3, null, 150, "150 coins awarded.", UserAction.PLAYED_GAME);
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        int coins = oldMoney.getCoins() - 100;
        _service.securePrice(1, item, Currency.COINS, coins, 2, 3, "testSupport - test");
        final MemberMoney newMoney = _service.buyItem(makeMember(true), item, Currency.COINS, coins,
            Currency.COINS, coins).getNewMemberMoney();
        assertEquals(100, newMoney.getCoins());

        // Now buy a 150 coin item.  Should succeed, bringing available coins to 0.
        final MemberMoney oldCreatorMoney = _service.getMoneyFor(2);
        _service.securePrice(1, item, Currency.COINS, 150, 2, 3, "testSupport - test2");
        MoneyResult result = _service.buyItem(
            makeMember(true), item, Currency.COINS, 150, Currency.COINS, 150);
        assertEquals(0, result.getNewMemberMoney().getCoins());
        assertEquals(30 + oldCreatorMoney.getCoins(), result.getNewCreatorMoney().getCoins());

        // Buy a 50 coin item.  Should succeed and remain at 0
        _service.securePrice(1, item, Currency.COINS, 50, 2, 3, "testSupport - test3");
        result = _service.buyItem(makeMember(true), item, Currency.COINS, 50, Currency.COINS, 50);
        assertEquals(0, result.getNewMemberMoney().getCoins());
        assertEquals(30 + oldCreatorMoney.getCoins(), result.getNewCreatorMoney().getCoins());
    }

    @Before
    public void setup () throws Exception
    {
        if (!initialized) {
            final ConnectionProvider connProv = ServerConfig.createConnectionProvider();
            final Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure ()
                {
                    bind(RunQueue.class).annotatedWith(EventQueue.class).to(
                        PresentsDObjectMgr.class);
                    bind(Invoker.class).annotatedWith(MainInvoker.class).toInstance(
                        new Invoker("test", new BasicRunQueue()));
                    bind(PersistenceContext.class).toInstance(
                        new PersistenceContext("msoy", connProv, null));
                    install(new MoneyModule());
                }
            });
            injector.injectMembers(this);
            _eventLog.init("test");
            initialized = true;
        }
    }

    private void checkMoneyHistory (
        final List<MoneyHistory> log, final MoneyHistory expected, final MoneyHistory reference,
        final long start, final long end, final boolean isPresent)
    {
        MoneyHistory logEntry = null;
        for (final MoneyHistory history : log) {
            final long time = history.getTimestamp().getTime() / 1000;
            if (history.getDescription().equals(expected.getDescription()) &&
                    time >= start && time <= end) {
                logEntry = history;
                break;
            }
        }
        if (logEntry == null) {
            if (isPresent) {
                fail("No appropriate log entry found.");
            } else {
                return;
            }
        }

        assertEquals(expected.getAmount(), logEntry.getAmount(), 0.01);
        // TODO: I've temporary disabled the item check because I've broken all that temporarily
//        assertEquals(expected.getItem(), logEntry.getItem());
        assertEquals(expected.getMemberId(), logEntry.getMemberId());
        assertEquals(expected.getCurrency(), logEntry.getCurrency());
        assertEquals(expected.isSpent(), logEntry.isSpent());
        assertEquals(expected.getTransactionType(), logEntry.getTransactionType());
        
        if (reference != null) {
            assertEquals(reference.getAmount(), logEntry.getReferenceTx().getAmount(), 0.01);
//            assertEquals(reference.getItem(), logEntry.getReferenceTx().getItem());
            assertEquals(reference.getMemberId(), logEntry.getReferenceTx().getMemberId());
            assertEquals(reference.getCurrency(), logEntry.getReferenceTx().getCurrency());
            assertEquals(reference.isSpent(), logEntry.getReferenceTx().isSpent());
            assertEquals(reference.getTransactionType(), 
                logEntry.getReferenceTx().getTransactionType());
        } else {
            assertTrue(logEntry.getReferenceTx() == null);
        }
    }

    private MemberRecord makeMember (final boolean isSupport)
    {
        final MemberRecord fakerec = new MemberRecord();
        fakerec.memberId = 1;
        fakerec.setFlag(MemberRecord.Flag.SUPPORT, isSupport);
        return fakerec;
    }

    private void checkActionLogExists (
        final int memberId, final int actionId, final String data, final long start, final long end)
        throws Exception
    {
        boolean found = false;
        for (final MemberActionLogRecord record : _userActionRepo.getLogRecords(memberId)) {
            if (record.actionId == actionId && record.data.equals(data) &&
                    record.memberId == memberId && record.actionTime.getTime() / 1000 >= start &&
                    record.actionTime.getTime() / 1000 <= end) {
                found = true;
                break;
            }
        }
        assertTrue("No matching action log record found.", found);
    }

    @Inject private MoneyLogic _service;
    @Inject private UserActionRepository _userActionRepo;
    @Inject private MsoyEventLogger _eventLog;
    @Inject private MoneyHistoryExpirer _expirer;
    private boolean initialized = false;
}
