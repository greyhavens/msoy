//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;

import net.jcip.annotations.NotThreadSafe;

import com.google.common.base.Preconditions;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.TransactionType;

/**
 * Domain model for the current status of a member's account, including the amount of each money
 * type currently in their account.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity(indices = { @Index(name = "ixVersion", fields = { MemberAccountRecord.VERSION_ID }) })
@NotThreadSafe
public class MemberAccountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberAccountRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #coins} field. */
    public static final String COINS = "coins";

    /** The qualified column identifier for the {@link #coins} field. */
    public static final ColumnExp COINS_C =
        new ColumnExp(MemberAccountRecord.class, COINS);

    /** The column identifier for the {@link #bars} field. */
    public static final String BARS = "bars";

    /** The qualified column identifier for the {@link #bars} field. */
    public static final ColumnExp BARS_C =
        new ColumnExp(MemberAccountRecord.class, BARS);

    /** The column identifier for the {@link #bling} field. */
    public static final String BLING = "bling";

    /** The qualified column identifier for the {@link #bling} field. */
    public static final ColumnExp BLING_C =
        new ColumnExp(MemberAccountRecord.class, BLING);

    /** The column identifier for the {@link #dateLastUpdated} field. */
    public static final String DATE_LAST_UPDATED = "dateLastUpdated";

    /** The qualified column identifier for the {@link #dateLastUpdated} field. */
    public static final ColumnExp DATE_LAST_UPDATED_C =
        new ColumnExp(MemberAccountRecord.class, DATE_LAST_UPDATED);

    /** The column identifier for the {@link #versionId} field. */
    public static final String VERSION_ID = "versionId";

    /** The qualified column identifier for the {@link #versionId} field. */
    public static final ColumnExp VERSION_ID_C =
        new ColumnExp(MemberAccountRecord.class, VERSION_ID);

    /** The column identifier for the {@link #accCoins} field. */
    public static final String ACC_COINS = "accCoins";

    /** The qualified column identifier for the {@link #accCoins} field. */
    public static final ColumnExp ACC_COINS_C =
        new ColumnExp(MemberAccountRecord.class, ACC_COINS);

    /** The column identifier for the {@link #accBars} field. */
    public static final String ACC_BARS = "accBars";

    /** The qualified column identifier for the {@link #accBars} field. */
    public static final ColumnExp ACC_BARS_C =
        new ColumnExp(MemberAccountRecord.class, ACC_BARS);

    /** The column identifier for the {@link #accBling} field. */
    public static final String ACC_BLING = "accBling";

    /** The qualified column identifier for the {@link #accBling} field. */
    public static final ColumnExp ACC_BLING_C =
        new ColumnExp(MemberAccountRecord.class, ACC_BLING);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 4;

    /** ID of the member this account record is for. */
    @Id
    public int memberId;

    /** Coins currently in the account. */
    public int coins;

    /** Bars currently in the account. */
    public int bars;

    /** Bling currently in the account. As usual, this is really "centibling" since we store
     * bling as a fixed-point number. 100 centibling == 1.00 bling. */
    public int bling;

    /** Date last updated. */
    public Timestamp dateLastUpdated;

    /** ID of the version of this account. */
    public long versionId;

    /** Cumulative count of coins this member has ever received. */
    public long accCoins;

    /** Cumulative count of bars this member has ever received. */
    public long accBars;

    /** Cumulative count of bling this member has ever received.
     * As usual, this is "centibling". */
    public long accBling;

    /**
     * Creates a new blank record for the given member. All account balances are set to 0.
     * 
     * @param memberId ID of the member to create the record for.
     */
    public MemberAccountRecord (final int memberId)
    {
        this.memberId = memberId;
        coins = 0;
        bars = 0;
        bling = 0;
        accCoins = 0;
        accBars = 0;
        accBling = 0;
        dateLastUpdated = new Timestamp(System.currentTimeMillis());
        versionId = 0;
    }

    /** For depot's eyes only. Not part of the API. */
    public MemberAccountRecord ()
    {
    }

    /**
     * Get the amount of money this account has of the specified currency.
     */
    public int getAmount (Currency currency)
    {
        switch (currency) {
        case COINS: return coins;
        case BARS: return bars;
        case BLING: return bling;
        default: throw new RuntimeException();
        }
    }

    /**
     * Returns true if the account can afford spending the amount of currency indicated.
     * 
     * @param currency Currency to spend, either BARS or COINS.
     * @param amount Amount to spend.
     * @return True if the account can afford it, false otherwise.
     */
    public boolean canAfford (Currency currency, int amount)
    {
        return getAmount(currency) >= amount;
    }

    /**
     * Called when someone makes money in such a way that we track all the money they ever
     * got. The amount must be positive, as we never deduct from the 'acc' values.
     */
    public void accumulate (Currency currency, int amount)
    {
        Preconditions.checkArgument(amount >= 0, "You may only accumulate positive wealth!");
        switch (currency) {
        case COINS:
            accCoins += amount;
            break;

        case BARS:
            accBars += amount;
            break;

        case BLING:
            accBling += amount;
            break;
        }

        adjust(currency, amount);
    }

    /**
     * Adjust the amount of money that a user has by the specified delta, without touching
     * the 'acc' values.
     */
    public void adjust (Currency currency, int delta)
    {
        switch (currency) {
        case COINS:
            coins += delta;
            break;

        case BARS:
            bars += delta;
            break;

        case BLING:
            bling += delta;
            break;
        }
    }

    /**
     * Adds the given number of bars to the member's account.
     * 
     * @param barsToAdd Number of bars to add.
     * @return Account history record for this transaction.
     */
    public MoneyTransactionRecord buyBars (int barsToAdd, String description)
    {
        accumulate(Currency.BARS, barsToAdd);
        return new MoneyTransactionRecord(memberId, TransactionType.BARS_BOUGHT,
            Currency.BARS, barsToAdd, bars, description, null);
    }

    /**
     * Adds the given number of coins to the member's account.
     * 
     * @param coins Number of coins to add.
     * @return Account history record for this transaction.
     */
    public MoneyTransactionRecord awardCoins (int coinsToAdd, ItemIdent item, String description)
    {
        accumulate(Currency.COINS, coinsToAdd);
        return new MoneyTransactionRecord(memberId, TransactionType.AWARD,
            Currency.COINS, coinsToAdd, coins, description, item);
    }

    /**
     * Purchases an item, deducting the appropriate amount of money from this account.
     * 
     * @param type Type indicating bars or coins.
     * @param amount Amount to deduct.
     * @param description Description that should be used in the history record.
     * @return Account history record for this transaction.
     */
    public MoneyTransactionRecord buyItem (
        Currency currency, int amount, String description, CatalogIdent item)
    {
        Preconditions.checkArgument(amount >= 0, "Buy amounts can't be negative");
        adjust(currency, -amount);
        return new MoneyTransactionRecord(memberId, TransactionType.ITEM_PURCHASE,
            currency, -amount, getAmount(currency), description, item);
    }

    /**
     * Pays the creator of an item purchased a certain percentage of the amount for the item.
     * 
     * @param type either TransactionType.CREATOR_PAYOUT or TransactionType.AFFILIATE_PAYOUT
     * @param amount Amount the item was worth.
     * @param listingType Money type the item was listed with.
     * @param description Description of the item purchased.
     * @param item Item that was purchased.
     * @return History record for the transaction.
     */
    public MoneyTransactionRecord payout (
        TransactionType type, float percentage,
        Currency listCurrency, Currency buyCurrency, int buyAmount,
        String description, CatalogIdent item, int referenceTxId)
    {
        int payAmount;
        if (listCurrency == buyCurrency) {
            payAmount = buyAmount;

        } else {
            // TODO: we need to convert this correctly, using the exchange rate, because
            // the buyAmount could have been adjusted since the PriceQuote was retrieved.
            // We may want to handle this back up in MoneyLogic...
            switch (listCurrency) {
            case COINS:
                payAmount = 1000 * buyAmount; // like I said, TODO
                break;

            case BARS:
                payAmount = buyAmount / 1000; // like I said, TODO
                break;

            default:
                throw new RuntimeException();
            }
        }

        Currency payoutCurrency;
        int payoutAmount;
        switch (listCurrency) {
        case COINS:
            payoutCurrency = Currency.COINS;
            payoutAmount = (int) Math.floor(payAmount * percentage);
            break;

        case BARS:
            payoutCurrency = Currency.BLING;
            payoutAmount = (int) Math.floor(payAmount * 100 * percentage); // it's really centibling 
            break;

        default:
            throw new RuntimeException();
        }

        // now, do the payout
        accumulate(payoutCurrency, payoutAmount);
        MoneyTransactionRecord trans = new MoneyTransactionRecord(
            memberId, type, payoutCurrency, payoutAmount, getAmount(payoutCurrency),
            description, item);
        trans.referenceTxId = referenceTxId;
        return trans;
    }

    /**
     * Creates a {@link MemberMoney} object from this record.
     */
    public MemberMoney getMemberMoney ()
    {
        return new MemberMoney(memberId, coins, bars, bling, accCoins, accBars, accBling);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #MemberAccountRecord}
     * with the supplied key values.
     */
    public static Key<MemberAccountRecord> getKey (int memberId)
    {
        return new Key<MemberAccountRecord>(
                MemberAccountRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
