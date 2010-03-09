// $Id: $

package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.List;

import com.google.inject.internal.Lists;
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.hadoop.KeyFactory;
import com.threerings.panopticon.aggregator.hadoop.KeyInitData;
import com.threerings.panopticon.aggregator.key.field.DayKey;
import com.threerings.panopticon.aggregator.result.StringInputNameResult;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.eventstore.EventWriter;

@Aggregator(output=DailyTransactions.OUTPUT_EVENT_NAME/*, incremental="timestamp" */)
public class DailyTransactions
    implements JavaAggregator<DailyTransactions.TransactionKey>,
               KeyFactory<DailyTransactions.TransactionKey>
{
    public static final String OUTPUT_EVENT_NAME = "DailyTransactions";

    enum Currency {
        COINS, BARS, BLING;
    }

    public static class TransactionKey extends DayKey
    {
        public int actionType;
        public Currency currency;

        public TransactionKey () { }

        public TransactionKey (EventData data, Currency currency)
        {
            init(data);

            this.actionType = data.getInt("actionType");
            this.currency = currency;
        }
    }

    @StringInputNameResult(inputs="FlowTransaction" /*, incrementals="timestamp" */)
    public static class Accumulation extends FieldAggregatedResult<TransactionKey>
    {
        public int earned;
        public int spent;

        @Override
        public void doInit (TransactionKey key, EventData eventData)
        {
            int amount;

            switch(key.currency) {
            case COINS:
            default:
                amount = eventData.getDefaultInt("deltaFlow", 0);
                break;
            case BARS:
                amount = eventData.getDefaultInt("deltaBars", 0);
                break;
            case BLING:
                amount = eventData.getDefaultInt("deltaBling", 0);
                break;
            }

            // store as appropriate
            if (amount > 0) {
                earned += amount;
            } else {
                spent -= amount;
            }
        }
    }

    public Accumulation result;

    @Override
    public List<TransactionKey> createKeys (KeyInitData keyInitData)
    {
        List<TransactionKey> keys = Lists.newArrayList();
        if (keyInitData.eventData.getDefaultInt("deltaFlow", 0) != 0) {
            keys.add(new TransactionKey(keyInitData.eventData, Currency.COINS));
        }
        if (keyInitData.eventData.getDefaultInt("deltaBars", 0) != 0) {
            keys.add(new TransactionKey(keyInitData.eventData, Currency.BARS));
        }
        if (keyInitData.eventData.getDefaultInt("deltaBling", 0) != 0) {
            keys.add(new TransactionKey(keyInitData.eventData, Currency.BLING));
        }
        return keys;
    }

    @Override
    public void write (EventWriter writer, EventDataBuilder builder, TransactionKey key)
        throws IOException
    {
        writer.write(builder.create(
            "timestamp", key.timestamp,
            "currency", key.currency.toString().toLowerCase(),
            "earned", result.earned,
            "spent", result.spent,
            "ratio", (result.spent == 0) ? 0 : (double) result.earned / result.spent));
    }
}
