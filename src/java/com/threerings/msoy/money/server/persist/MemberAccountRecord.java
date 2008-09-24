//
// $Id$

package com.threerings.msoy.money.server.persist;

import net.jcip.annotations.NotThreadSafe;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

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

    public static final int SCHEMA_VERSION = 6;

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

    /**
     * Return the column name for the specified currency.
     */
    public static ColumnExp getColumn (Currency currency)
    {
        switch (currency) {
        case COINS: return COINS_C;
        case BARS: return BARS_C;
        case BLING: return BLING_C;
        default: throw new IllegalArgumentException();
        }
    }

    /**
     * Return the column name for the specified currency.
     */
    public static ColumnExp getAccColumn (Currency currency)
    {
        switch (currency) {
        case COINS: return ACC_COINS_C;
        case BARS: return ACC_BARS_C;
        case BLING: return ACC_BLING_C;
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
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
