package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.samskivert.util.StringUtil;
import com.threerings.panopticon.aggregator.Schedule;
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.result.Result;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.aggregator.result.field.FieldKey;
import com.threerings.panopticon.aggregator.result.field.FieldWritable;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.efs.storev2.EventWriter;
import com.threerings.panopticon.efs.storev2.StorageStrategy;

@Aggregator(outputs=AccountsWithVectors.OUTPUT_EVENT_NAME, schedule=Schedule.NIGHTLY,
    nexts=DailyAccountsCreated.class)
public class AccountsWithVectors
    implements JavaAggregator<AccountsWithVectors.TrackerKey>
{
    public static final String OUTPUT_EVENT_NAME = "AccountsWithVectors";

    public static class TrackerKey extends FieldKey
    {
        public String tracker;

        @Override
        public void init (EventData eventData)
        {
            tracker = (String)eventData.get("tracker");
        }
    }
    
    @Result(inputs="VectorAssociated")
    public static class VectorMap extends FieldAggregatedResult 
    {
        public Map<String, String> trackerToVector = Maps.newHashMap();

        @Override
        public void doInit (EventData eventData)
        {
            String tracker = eventData.getString("tracker");
            String vector = eventData.getString("vector");
            if (tracker != null && vector != null) {
                int ampersand = vector.indexOf("&");
                if (ampersand > -1) {
                    // remove any parameters that got passed in accidentally
                    vector = vector.substring(0, ampersand);
                }
                trackerToVector.put(tracker, vector);
            }
        }

        @Override
        public void combine (FieldAggregatedResult result)
        {
            // we're really not supposed to have multiple vectors per tracker, so just overwrite
            trackerToVector.putAll(((VectorMap) result).trackerToVector);
        }
    }

    @Result(inputs="AccountCreated")
    public static class AccountMap extends FieldAggregatedResult
    {
        public Map<String, Account> trackerToAccount = Maps.newHashMap();

        @Override
        public void doInit (EventData eventData)
        {
            Boolean isGuestValue = (Boolean)eventData.getData().get("isGuest");

            if (isGuestValue == null || !isGuestValue.booleanValue()) {
                trackerToAccount.put(eventData.getString("tracker"), new Account(
                    eventData.getInt("newMemberId"),
                    eventData.getDate("timestamp"),
                    eventData.getInt("affiliateId")));
            }
        }
        
        @Override
        public void combine (FieldAggregatedResult result)
        {
            // we're really not supposed to have multiple accounts per tracker, so just overwrite
            trackerToAccount.putAll(((AccountMap) result).trackerToAccount);
        }
    }
    
    protected static class Account extends FieldWritable
    {
        public int memberId;
        public Date date;
        public int affiliateId;

        public Account ()
        {
            // empty constructor for instantiation
        }
        
        public Account (int memberId, Date date, int affiliateId)
        {
            this.memberId = memberId;
            this.date = date;
            this.affiliateId = affiliateId;
        }
    }
    
    /** Builds a mapping of tracker -> vector */
    public VectorMap vectors;
    
    /** Builds a mapping of tracker -> (memberId, date, affiliateId) */
    public AccountMap accounts;
    
    public void write (EventWriter writer, TrackerKey key)
        throws IOException
    {
        Account account = accounts.trackerToAccount.get(key.tracker);
        String vector = StringUtil.deNull(vectors.trackerToVector.get(key.tracker));
        if (account == null) {
            return;
        }
        EventData event = new EventData(OUTPUT_EVENT_NAME, new ImmutableMap.Builder<String, Object>()
            .put("tracker", key.tracker)
            .put("memberId", account.memberId)
            .put("date", account.date)
            .put("affiliateId", account.affiliateId)
            .put("vector", vector).build(), new HashMap<String, Object>());
        writer.write(event, StorageStrategy.PROCESSED);
    }
}
