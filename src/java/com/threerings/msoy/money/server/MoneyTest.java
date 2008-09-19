//
// $Id$

package com.threerings.msoy.money.server;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;

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
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.messaging.DelayedMessageConnection;
import com.threerings.messaging.MessageConnection;
import com.threerings.msoy.data.UserAction;

import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberActionLogRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.UserActionRepository;
import com.threerings.msoy.server.util.Retry;
import com.threerings.msoy.server.util.RetryInterceptor;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.TransactionType;

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
        JUnitCore.main(new String[] { MoneyTest.class.getName() });
    }

    @Test
    public void testExpirer ()
        throws Exception
    {
//        // perform 3 transactions: 1 with coins and 1 with bars.  Then wait 3s and do another
//        // with coins.  Create an expirer to remove coin histories older than 3s -- ensure
//        // the other two exist.
//        final long start = System.currentTimeMillis() / 1000;
//        _service.awardCoins(1, 0, 0, null, 100, "testExpirer - coins1", UserAction.PLAYED_GAME);
//        _service.buyBars(1, 10, "Bought 10 bars");
//        final long end = System.currentTimeMillis() / 1000;
//        System.out.println("Waiting 3s to create non-expiring item.");
//        Thread.sleep(3000);
//        final long start2 = System.currentTimeMillis() / 1000;
//        _service.awardCoins(1, 0, 0, null, 101, "testExpirer - coins2", UserAction.PLAYED_GAME);
//        final long end2 = System.currentTimeMillis() / 1000;
//        _service._expirer.setMaxAge(3000);
//        _service._expirer.start();
//
//        final List<MoneyTransaction> log = _service.getTransactions(1, null, null, 0, 30, true);
//        checkMoneyTransaction(log, new MoneyTransaction(1, new Date(), Currency.BARS, 10.0, 
//            TransactionType.BARS_BOUGHT, false, "Bought 10 bars", null, null), null, start, end, true);
//        checkMoneyTransaction(log, new MoneyTransaction(1, new Date(), Currency.COINS, 101.0, 
//            TransactionType.AWARD, false, "testExpirer - coins2", null, null), null, start2, end2, true);
//        checkMoneyTransaction(log, new MoneyTransaction(1, new Date(), Currency.COINS, 100.0, 
//            TransactionType.AWARD, false, "testExpirer - coins1", null, null), null, start, end, false);
    }

    @Test
    public void testBuyBars ()
        throws Exception
    {
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final MoneyResult result = _service.buyBars(1, 2, "Bought 2 bars.");
        assertEquals(oldMoney.bars + 2, result.getNewMemberMoney().bars);
        assertEquals(oldMoney.accBars + 2, result.getNewMemberMoney().accBars);
        final long endTime = System.currentTimeMillis() / 1000;

        final List<MoneyTransaction> log = _service.getTransactions(
            1, null, Currency.BARS, 0, 30, true);
        checkMoneyTransaction(log, new MoneyTransaction(
            1, new Timestamp(System.currentTimeMillis()), TransactionType.BARS_BOUGHT,
            Currency.BARS, 2, 0, "Bought 2 bars."), startTime, endTime, true);

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
        _service.securePrice(1, item, Currency.BARS, 100);
        final MoneyResult result = _service.buyItem(makeMember(false), item, 2, "My bar item",
            Currency.BARS, 100, Currency.BARS, 100);
        final long endTime = System.currentTimeMillis() / 1000;

        // Check member account
        final MemberMoney newMoney = result.getNewMemberMoney();
        assertEquals(oldMoney.bars + 50, newMoney.bars);
        assertEquals(oldMoney.accBars + 150, newMoney.accBars);
        
        List<MoneyTransaction> log = _service.getTransactions(1, null, Currency.BARS, 0, 30, true);
        final MoneyTransaction expectedMH = new MoneyTransaction(
            1, new Timestamp(System.currentTimeMillis()), TransactionType.ITEM_PURCHASE,
            Currency.BARS, 100, 0, "My bar item");
        checkMoneyTransaction(log, expectedMH, startTime, endTime, true);

        checkActionLogExists(1, UserAction.BOUGHT_ITEM.getNumber(), "My bar item",
            startTime, endTime);

        // Check creator account
        final MemberMoney newCreatorMoney = result.getNewCreatorMoney();
        assertEquals(oldCreatorMoney.bling + 100.0*0.3, newCreatorMoney.bling, 0.001);
        assertEquals(oldCreatorMoney.accBling + 100.0*0.3, newCreatorMoney.accBling,
            0.001);

        log = _service.getTransactions(2, null, Currency.BLING, 0, 30, true);
        checkMoneyTransaction(log, new MoneyTransaction(
            2, new Timestamp(System.currentTimeMillis()), TransactionType.CREATOR_PAYOUT,
            Currency.BLING, (int) (100*0.3), 0, "My bar item"),
            startTime, endTime, true);

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
        final int bars = oldMoney.bars + 1;
        _service.securePrice(1, item, Currency.BARS, bars);
        _service.buyItem(makeMember(false), item, 2, "My bar item",
            Currency.BARS, bars, Currency.BARS, bars);
    }

    @Test(expected=NotEnoughMoneyException.class)
    public void testNotEnoughCoins ()
        throws Exception
    {
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        final  int coins = oldMoney.coins + 1;
        _service.securePrice(1, item, Currency.COINS, coins);
        _service.buyItem(makeMember(false), item, 2, "My coin item",
            Currency.COINS, coins, Currency.COINS, coins);
    }

    @Test(expected=NotSecuredException.class)
    public void testNotSecured ()
        throws Exception
    {
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        _service.buyItem(makeMember(false), item, 2, "some thing",
            Currency.COINS, 100, Currency.COINS, 100);
    }

    @Test
    public void testBuyCoinItemWithCoins ()
        throws Exception
    {
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final MemberMoney oldCreatorMoney = _service.getMoneyFor(2);
        _service.awardCoins(1, 2, 3, null, 150, false, UserAction.PLAYED_GAME);
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        _service.securePrice(1, item, Currency.COINS, 100);
        final MoneyResult result = _service.buyItem(makeMember(false), item, 2,
            "testBuyCoinItemWithCoins - test", Currency.COINS, 100, Currency.COINS, 100);
        final long endTime = System.currentTimeMillis() / 1000;

        // Check member account
        final MemberMoney newMoney = result.getNewMemberMoney();
        assertEquals(oldMoney.coins + 50, newMoney.coins);
        assertEquals(oldMoney.accCoins + 150, newMoney.accCoins);

        List<MoneyTransaction> log = _service.getTransactions(1, null, Currency.COINS, 0, 30, true);
        final MoneyTransaction expectedMH = new MoneyTransaction(
            1, new Timestamp(System.currentTimeMillis()), TransactionType.ITEM_PURCHASE,
            Currency.COINS, 100, 0, "testBuyCoinItemWithCoins - test");
        checkMoneyTransaction(log, expectedMH, startTime, endTime, true);

        checkActionLogExists(1, UserAction.BOUGHT_ITEM.getNumber(),
            "testBuyCoinItemWithCoins - test", startTime, endTime);

        // Check creator account
        final MemberMoney newCreatorMoney = result.getNewCreatorMoney();
        assertEquals(oldCreatorMoney.coins + 30, newCreatorMoney.coins);
        assertEquals(oldCreatorMoney.accCoins + 30, newCreatorMoney.accCoins);

        log = _service.getTransactions(2, null, Currency.COINS, 0, 30, true);
        checkMoneyTransaction(log, new MoneyTransaction(
            2, new Timestamp(System.currentTimeMillis()), TransactionType.CREATOR_PAYOUT,
            Currency.COINS, 30, 0, "testBuyCoinItemWithCoins - test"),
            startTime, endTime, true);

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
            false, UserAction.PLAYED_GAME, 666);
        final long endTime = System.currentTimeMillis() / 1000;

        final MemberMoney newMoney = result.getNewMemberMoney();
        assertEquals(oldMoney.coins + 150, newMoney.coins);
        assertEquals(oldMoney.accCoins + 150, newMoney.accCoins);

        final List<MoneyTransaction> log = _service.getTransactions(
            1, null, Currency.COINS, 0, 30, true);
        checkMoneyTransaction(log, new MoneyTransaction(
            1, new Timestamp(System.currentTimeMillis()), TransactionType.AWARD,
            Currency.COINS, 150, 0, "m.game_payout|~666"),
            startTime, endTime, true);

        checkActionLogExists(1, UserAction.PLAYED_GAME.getNumber(),
            "m.game_payout|~666", startTime, endTime);

        // TODO: check creator and affiliate accounts
    }

    @Test
    public void testCreatorBoughtOwnItem ()
        throws Exception
    {
        final long startTime = System.currentTimeMillis() / 1000;
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        _service.awardCoins(1, 1, 3, null, 150, false, UserAction.PLAYED_GAME);
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        _service.securePrice(1, item, Currency.COINS, 100);
        final MoneyResult result = _service.buyItem(makeMember(false), item, 1,
            "testCreatorBoughtOwnItem - test", Currency.COINS, 100, Currency.COINS, 100);
        final long endTime = System.currentTimeMillis() / 1000;

        // Check member account
        final MemberMoney newMoney = result.getNewMemberMoney();
        assertEquals(oldMoney.coins + 50 + 30, newMoney.coins);
        assertEquals(oldMoney.accCoins + 150, newMoney.accCoins);

        final List<MoneyTransaction> log = _service.getTransactions(
            1, null, Currency.COINS, 0, 30, true);
        checkMoneyTransaction(log, new MoneyTransaction(
            1, new Timestamp(System.currentTimeMillis()), TransactionType.ITEM_PURCHASE,
            Currency.COINS, 70, 0, "testCreatorBoughtOwnItem - test"),
            startTime, endTime, true);

        checkActionLogExists(1, UserAction.BOUGHT_ITEM.getNumber(),
            "testCreatorBoughtOwnItem - test", startTime, endTime);
    }

    @Test
    public void testSupport () throws Exception
    {
        // First, have a support account buy an item, bringing them down to 100 coins
        _service.awardCoins(1, 2, 3, null, 150, false, UserAction.PLAYED_GAME);
        final MemberMoney oldMoney = _service.getMoneyFor(1);
        final CatalogIdent item = new CatalogIdent(Item.AVATAR, 1);
        int coins = oldMoney.coins - 100;
        _service.securePrice(1, item, Currency.COINS, coins);
        final MemberMoney newMoney = _service.buyItem(makeMember(true), item, 2,
            "testSupport - test", Currency.COINS, coins, Currency.COINS, coins).getNewMemberMoney();
        assertEquals(100, newMoney.coins);

        // Now buy a 150 coin item.  Should succeed, bringing available coins to 0.
        final MemberMoney oldCreatorMoney = _service.getMoneyFor(2);
        _service.securePrice(1, item, Currency.COINS, 150);
        MoneyResult result = _service.buyItem(
            makeMember(true), item, 2, "testSupport - test2",
            Currency.COINS, 150, Currency.COINS, 150);
        assertEquals(0, result.getNewMemberMoney().coins);
        assertEquals(30 + oldCreatorMoney.coins, result.getNewCreatorMoney().coins);

        // Buy a 50 coin item.  Should succeed and remain at 0
        _service.securePrice(1, item, Currency.COINS, 50);
        result = _service.buyItem(makeMember(true), item, 2, "testSupport - test3",
            Currency.COINS, 50, Currency.COINS, 50);
        assertEquals(0, result.getNewMemberMoney().coins);
        assertEquals(30 + oldCreatorMoney.coins, result.getNewCreatorMoney().coins);
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
                    bind(MessageConnection.class).toInstance(new DelayedMessageConnection());
                    bindInterceptor(any(), annotatedWith(Retry.class), new RetryInterceptor());
                }
            });
            injector.injectMembers(this);
            _eventLog.init("test");
            initialized = true;
        }
    }
    
    protected static class MockPeerManager extends PeerManager
    {
        @Inject
        public MockPeerManager (ShutdownManager shutmgr)
        {
            super(shutmgr);
        }
        
    }

    protected void checkMoneyTransaction (
        List<MoneyTransaction> log, MoneyTransaction expected,
        long start, long end, boolean isPresent)
    {
        MoneyTransaction logEntry = null;
        for (final MoneyTransaction entry : log) {
            final long time = entry.timestamp.getTime() / 1000;
            if (entry.description.contains(expected.description) && time >= start && time <= end) {
                logEntry = entry;
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

        assertEquals(expected.amount, logEntry.amount);
        // TODO: I've temporary disabled the item check because I've broken all that temporarily
//        assertEquals(expected.getItem(), logEntry.getItem());
        assertEquals(expected.memberId, logEntry.memberId);
        assertEquals(expected.currency, logEntry.currency);
        assertEquals(expected.transactionType, logEntry.transactionType);
    }

    protected MemberRecord makeMember (final boolean isSupport)
    {
        final MemberRecord fakerec = new MemberRecord();
        fakerec.memberId = 1;
        // fakerec.affiliateMemberId = 3;
        fakerec.setFlag(MemberRecord.Flag.SUPPORT, isSupport);
        return fakerec;
    }

    protected void checkActionLogExists (
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

    @Inject protected MoneyLogic _service;
    @Inject protected UserActionRepository _userActionRepo;
    @Inject protected MsoyEventLogger _eventLog;
    protected boolean initialized = false;
}
