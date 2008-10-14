//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.money.data.all.ExchangeData;

/**
 * Records money exchanges.
 */
@Entity(indices = {
    @Index(name = "ixTimestamp", fields = { ExchangeRecord.TIMESTAMP }) })
public class ExchangeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #id} field. */
    public static final String ID = "id";

    /** The qualified column identifier for the {@link #id} field. */
    public static final ColumnExp ID_C =
        new ColumnExp(ExchangeRecord.class, ID);

    /** The column identifier for the {@link #timestamp} field. */
    public static final String TIMESTAMP = "timestamp";

    /** The qualified column identifier for the {@link #timestamp} field. */
    public static final ColumnExp TIMESTAMP_C =
        new ColumnExp(ExchangeRecord.class, TIMESTAMP);

    /** The column identifier for the {@link #bars} field. */
    public static final String BARS = "bars";

    /** The qualified column identifier for the {@link #bars} field. */
    public static final ColumnExp BARS_C =
        new ColumnExp(ExchangeRecord.class, BARS);

    /** The column identifier for the {@link #coins} field. */
    public static final String COINS = "coins";

    /** The qualified column identifier for the {@link #coins} field. */
    public static final ColumnExp COINS_C =
        new ColumnExp(ExchangeRecord.class, COINS);

    /** The column identifier for the {@link #rate} field. */
    public static final String RATE = "rate";

    /** The qualified column identifier for the {@link #rate} field. */
    public static final ColumnExp RATE_C =
        new ColumnExp(ExchangeRecord.class, RATE);

    /** The column identifier for the {@link #referenceTxId} field. */
    public static final String REFERENCE_TX_ID = "referenceTxId";

    /** The qualified column identifier for the {@link #referenceTxId} field. */
    public static final ColumnExp REFERENCE_TX_ID_C =
        new ColumnExp(ExchangeRecord.class, REFERENCE_TX_ID);
    // AUTO-GENERATED: FIELDS END

    /** Increment when you change fields. */
    public static final int SCHEMA_VERSION = 3;

    /** The ID of this record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    /** The time at which the exchange was performed. */
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
                new String[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END
}
