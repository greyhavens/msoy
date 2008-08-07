//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import net.jcip.annotations.NotThreadSafe;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.money.server.MemberMoney;
import com.threerings.msoy.money.server.MoneyType;

/**
 * Domain model for the current status of a member's account, including the amount of
 * each money type currently in their account.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity(indices={
    @Index(name="ixVersion", fields={ MemberAccountRecord.VERSION_ID })
})
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
    
    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #MemberAccountRecord}
     * with the supplied key values.
     */
    public static Key<MemberAccountRecord> getKey (final int memberId)
    {
        return new Key<MemberAccountRecord>(
                MemberAccountRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
    
    public static final int SCHEMA_VERSION = 2;
    
    /**
     * Creates a new blank record for the given member.  All account balances are set to 0.
     * 
     * @param memberId ID of the member to create the record for.
     */
    public MemberAccountRecord (final int memberId)
    {
        this.memberId = memberId;
        this.coins = 0;
        this.bars = 0;
        this.bling = 0.0;
        this.accCoins = 0;
        this.accBars = 0;
        this.accBling = 0.0;
        this.dateLastUpdated = new Timestamp(new Date().getTime());
        this.versionId = 0;
    }
    
    /** For depot's eyes only.  Not part of the API. */
    public MemberAccountRecord ()
    {
    }
    
    /**
     * Adds the given number of bars to the member's account.
     * 
     * @param bars Number of bars to add.
     * @return Account history record for this transaction.
     */
    public MemberAccountHistoryRecord buyBars (final int bars)
    {
        this.bars += bars;
        this.accBars += bars;
        this.dateLastUpdated = new Timestamp(new Date().getTime());
        return new MemberAccountHistoryRecord(this.memberId, this.dateLastUpdated, MoneyType.BARS,
            bars, false, "Purchased " + bars + " bars.");
    }
    
    /**
     * Adds the given number of coins to the member's account.
     * 
     * @param coins Number of coins to add.
     * @return Account history record for this transaction.
     */
    public MemberAccountHistoryRecord awardCoins (final int coins)
    {
        this.coins += coins;
        this.accCoins += coins;
        this.dateLastUpdated = new Timestamp(new Date().getTime());
        return new MemberAccountHistoryRecord(this.memberId, this.dateLastUpdated, MoneyType.COINS,
            coins, false, "Awarded " + coins + " coins.");
    }
    
    /**
     * Purchases an item, deducting the appropriate amount of money from this account.
     * 
     * @param amount Amount to deduct.
     * @param type Type indicating bars or coins.
     * @param description Description that should be used in the history record.
     * @return Account history record for this transaction.
     */
    public MemberAccountHistoryRecord buyItem (final int amount, final MoneyType type, final String description,
        final ItemIdent item)
    {
        if (type == MoneyType.BARS) {
            this.bars -= amount;
        } else {
            this.coins -= amount;
        }
        this.dateLastUpdated = new Timestamp(new Date().getTime());
        return new MemberAccountHistoryRecord(memberId, dateLastUpdated, type, amount, true, 
            description, item.itemId, item.type);
    }
    
    /**
     * Returns true if the account can afford spending the amount of currency indicated.
     * 
     * @param amount Amount to spend.
     * @param type Currency to spend, either BARS or COINS.
     * @return True if the account can afford it, false otherwise.
     */
    public boolean canAfford (final int amount, final MoneyType type)
    {
        return type == MoneyType.BARS ? (bars >= amount) : (coins >= amount);
    }
    
    /**
     * Pays the creator of an item purchased a certain percentage of the amount for the item.
     * 
     * @param amount Amount the item was worth.
     * @param listingType Money type the item was listed with.
     * @param description Description of the item purchased.
     * @param item Item that was purchased.
     * @return History record for the transaction.
     */
    public MemberAccountHistoryRecord creatorPayout (final int amount, final MoneyType listingType, 
        final String description, final ItemIdent item)
    {
        // TODO: Determine percentage from administrator.
        final double amountPaid = 0.3 * amount;
        final MoneyType paymentType;
        if (listingType == MoneyType.BARS) {
            this.bling += amountPaid;
            this.accBling += amountPaid;
            paymentType = MoneyType.BLING;
        } else {
            this.coins += (int)amountPaid;
            this.accCoins += (int)amountPaid;
            paymentType = MoneyType.COINS;
        }
        this.dateLastUpdated = new Timestamp(new Date().getTime());
        return new MemberAccountHistoryRecord(memberId, dateLastUpdated, paymentType, amountPaid, false, 
            description);
    }
    
    public int getMemberId ()
    {
        return memberId;
    }

    public int getCoins ()
    {
        return coins;
    }

    public int getBars ()
    {
        return bars;
    }

    public double getBling ()
    {
        return bling;
    }

    public Date getDateLastUpdated ()
    {
        return dateLastUpdated;
    }

    public long getVersionId ()
    {
        return versionId;
    }

    public long getAccCoins ()
    {
        return accCoins;
    }

    public long getAccBars ()
    {
        return accBars;
    }

    public double getAccBling ()
    {
        return accBling;
    }
    
    /**
     * Creates a {@link MemberMoney} object from this record.
     */
    public MemberMoney getMemberMoney ()
    {
        return new MemberMoney(memberId, coins, bars, bling, accCoins, accBars, accBling);
    }
    
    /** ID of the member this account record is for.  Note: this is not part of the API, do not use it. */
    @Id
    public int memberId;
    
    /** Coins currently in the account.  Note: this is not part of the API, do not use it. */
    public int coins;
    
    /** Bars currently in the account.  Note: this is not part of the API, do not use it. */
    public int bars;
    
    /** Bling currently in the account.  Note: this is not part of the API, do not use it. */
    public double bling;
    
    /** Date last updated.  Note: this is not part of the API, do not use it.  Also, why does depot force 
     * this dependency on java.sql in the entity object?  :-( */
    public Timestamp dateLastUpdated;
    
    /** ID of the version of this account.  Note: this is not part of the API, do not use it. */
    public long versionId;
    
    /** Cumulative count of coins this member has ever received.  Note: this is not part of the API, do
     * not use it. */
    public long accCoins;
    
    /** Cumulative count of bars this member has ever received.  Note: this is not part of the API, do
     * not use it. */
    public long accBars;
    
    /** Cumulative count of bling this member has ever received.  Note: this is not part of the API, do
     * not use it. */
    public double accBling;
}
