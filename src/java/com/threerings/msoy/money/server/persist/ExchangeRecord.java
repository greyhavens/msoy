//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.money.data.all.ExchangeData;

/**
 * Records money exchanges.
 */
@Entity
public class ExchangeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ExchangeRecord> _R = ExchangeRecord.class;
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp TIMESTAMP = colexp(_R, "timestamp");
    public static final ColumnExp BARS = colexp(_R, "bars");
    public static final ColumnExp COINS = colexp(_R, "coins");
    public static final ColumnExp RATE = colexp(_R, "rate");
    public static final ColumnExp REFERENCE_TX_ID = colexp(_R, "referenceTxId");
    // AUTO-GENERATED: FIELDS END

    /** Increment when you change fields. */
    public static final int SCHEMA_VERSION = 2;

    /** The ID of this record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    /** The time at which the exchange was performed. */
    @Index(name="ixTimestamp")
    public Timestamp timestamp;

    /** The number of bars created or destroyed. */
    public int bars;

    /** The number of coins created or destroyed. */
    public int coins;

    /** The exact exchange rate that was used for this exchange. */
    public double rate;

    /** The reference id of the MoneyTransaction. */
    public int referenceTxId;

    /** Converts ExchangeRecords to ExchangeData */
    public static Function<ExchangeRecord, ExchangeData> TO_EXCHANGE_DATA =
        new Function<ExchangeRecord, ExchangeData>() {
            public ExchangeData apply (ExchangeRecord record) {
                return record.toData();
            }
        };

    /**
     * Construct a new ExchangeRecord.
     */
    public ExchangeRecord (int bars, int coins, float rate, int referenceTxId)
    {
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.bars = bars;
        this.coins = coins;
        // NOTE: for some fucked-up reason when this column was a Float I couldn't store
        // MAX_VALUE, but I can as a double. I can't store an INFINITY either way, so we
        // convert Float's infinity into double's MAX_VALUE and decode on the other end
        this.rate = (rate == Float.POSITIVE_INFINITY) ? Double.MAX_VALUE : rate;
        this.referenceTxId = referenceTxId;
    }

    /** Suitable for unserialization. */
    public ExchangeRecord ()
    {
    }

    public ExchangeData toData ()
    {
        return new ExchangeData(timestamp, bars, coins,
            // Decode the rate.. Ferfuck's sake
            (rate == Double.MAX_VALUE) ? Float.POSITIVE_INFINITY : (float)rate,
            referenceTxId);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ExchangeRecord}
     * with the supplied key values.
     */
    public static Key<ExchangeRecord> getKey (int id)
    {
        return new Key<ExchangeRecord>(
                ExchangeRecord.class,
                new ColumnExp[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END
}
