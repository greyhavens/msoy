//
// $Id$

package com.threerings.msoy.money.server.persist;

import net.jcip.annotations.NotThreadSafe;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.Currency;

/**
 * Domain model for the current status of a member's account, including the amount of each money
 * type currently in their account.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity
@NotThreadSafe
public class MemberAccountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MemberAccountRecord> _R = MemberAccountRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp COINS = colexp(_R, "coins");
    public static final ColumnExp BARS = colexp(_R, "bars");
    public static final ColumnExp BLING = colexp(_R, "bling");
    public static final ColumnExp ACC_COINS = colexp(_R, "accCoins");
    public static final ColumnExp ACC_BARS = colexp(_R, "accBars");
    public static final ColumnExp ACC_BLING = colexp(_R, "accBling");
    public static final ColumnExp CASH_OUT_BLING = colexp(_R, "cashOutBling");
    public static final ColumnExp CASH_OUT_BLING_WORTH = colexp(_R, "cashOutBlingWorth");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 7;

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

    /** Cumulative count of coins this member has ever received. */
    public long accCoins;

    /** Cumulative count of bars this member has ever received. */
    public long accBars;

    /** Cumulative count of bling this member has ever received.
     * As usual, this is "centibling". */
    public long accBling;

    /** The amount of bling the user has requested to cash out, or 0 if no cash out. */
    public int cashOutBling;

    /** The worth of each bling when the user requested a cash out. */
    public float cashOutBlingWorth;

    /**
     * Return the column name for the specified currency.
     */
    public static ColumnExp getColumn (Currency currency)
    {
        switch (currency) {
        case COINS: return COINS;
        case BARS: return BARS;
        case BLING: return BLING;
        default: throw new IllegalArgumentException();
        }
    }

    /**
     * Return the column name for the specified currency.
     */
    public static ColumnExp getAccColumn (Currency currency)
    {
        switch (currency) {
        case COINS: return ACC_COINS;
        case BARS: return ACC_BARS;
        case BLING: return ACC_BLING;
        default: throw new IllegalArgumentException();
        }
    }

    /**
     * Creates a new blank record for the given member. All account balances are set to 0.
     *
     * @param memberId ID of the member to create the record for.
     */
    public MemberAccountRecord (int memberId)
    {
        this.memberId = memberId;
        coins = 0;
        bars = 0;
        bling = 0;
        accCoins = 0;
        accBars = 0;
        accBling = 0;
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
        default: throw new IllegalArgumentException();
        }
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
     * Create and return a primary {@link Key} to identify a {@link MemberAccountRecord}
     * with the supplied key values.
     */
    public static Key<MemberAccountRecord> getKey (int memberId)
    {
        return new Key<MemberAccountRecord>(
                MemberAccountRecord.class,
                new ColumnExp[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
