// $Id: $

package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

@Aggregator(output=DailyTransactions.OUTPUT_EVENT_NAME, incremental="timestamp")
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
        public Currency currency;

        public TransactionKey () { }

        public TransactionKey (EventData data, Currency currency)
        {
            init(data);
            this.currency = currency;
        }
    }

    @StringInputNameResult(inputs="FlowTransaction", incrementals="timestamp")
    public static class Accumulation extends FieldAggregatedResult<TransactionKey>
    {
        public Map<Integer, Integer> earned = Maps.newHashMap();
        public Map<Integer, Integer> spent = Maps.newHashMap();

        @Override
        public void doInit (TransactionKey key, EventData eventData)
        {
            if (!eventData.containsKey("actionType")) {
                return;
            }

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
                earned.put(eventData.getInt("actionType"), amount);

            } else {
                spent.put(eventData.getInt("actionType"), amount);
            }
        }
    }

    public Accumulation result;

    @Override
    public List<TransactionKey> createKeys (KeyInitData keyInitData)
    {
        List<TransactionKey> keys = Lists.newArrayList();
        if (ACTION_TYPES.contains(keyInitData.eventData.getInt("actionType"))) {
            if (keyInitData.eventData.getDefaultInt("deltaFlow", 0) != 0) {
                keys.add(new TransactionKey(keyInitData.eventData, Currency.COINS));
            }
            if (keyInitData.eventData.getDefaultInt("deltaBars", 0) != 0) {
                keys.add(new TransactionKey(keyInitData.eventData, Currency.BARS));
            }
            if (keyInitData.eventData.getDefaultInt("deltaBling", 0) != 0) {
                keys.add(new TransactionKey(keyInitData.eventData, Currency.BLING));
            }
        }
        return keys;
    }

    @Override
    public void write (EventWriter writer, EventDataBuilder builder, TransactionKey key)
        throws IOException
    {
        if (result.earned.isEmpty() && result.spent.isEmpty()) {
            return;
        }
        Map<String, Object> props = Maps.newHashMap();
        for (int actionType : ACTION_TYPES) {
            int earned = toValue(result.earned.get(actionType));
            int spent = toValue(result.spent.get(actionType));

            props.put("earned:" + actionType, earned);
            props.put("spent:" + actionType, spent);
            props.put("ratio:" + actionType, (spent == 0) ? 0 : earned / spent);
        }
        props.put("timestamp", key.timestamp);
        props.put("currency", key.currency.toString().toLowerCase());

        writer.write(builder.create(props));
    }

    protected static int toValue (Integer maybeValue)
    {
        return (maybeValue != null) ? maybeValue.intValue() : 0;
    }

    protected static final Set<Integer> ACTION_TYPES =
        ImmutableSet.of(20, 31, 34, 40, 50, 51, 54, 55);
}
