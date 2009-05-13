//
// $Id$

package com.threerings.msoy.aggregators.result;

import java.util.Map;

import com.google.common.collect.Maps;

import com.threerings.panopticon.aggregator.result.AggregatedResult;
import com.threerings.panopticon.aggregator.result.StringInputNameResult;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.aggregator.result.field.FieldWritable;
import com.threerings.panopticon.common.event.EventData;

/**
 * Processes all the original retention email events and outputs a map of member id to mailing
 * data.
 */
@StringInputNameResult(inputs="RetentionMailSent") // original msoy event
public class RetentionEmailResult extends FieldAggregatedResult
{
    /**
     * Normalized msoy retention email event.
     */
    public static class Mailing extends FieldWritable
        implements AggregatedResult<Mailing>
    {
        public String bucket;
        public String subject;
        public Boolean validated;
        public Integer memberId;

        public boolean init (EventData eventData) {
            memberId = eventData.getInt("recipientId");
            bucket = eventData.getDefaultString("bucket", "default");
            subject = eventData.getDefaultString("subjectLine", "default");
            validated = eventData.getDefaultBoolean("validated", false);
            return false;
        }

        public void combine (Mailing value) {
            throw new UnsupportedOperationException("Duplicate mailings to same memberId");
        }

        public boolean putData (Map<String, Object> result) {
            throw new UnsupportedOperationException("Not for use by aggregator config");
        }
    }

    /** The mailings, mapped by member id. */
    public Map<Integer, Mailing> sent = Maps.newHashMap();

    public static boolean checkInputs (EventData eventData)
    {
        return checkInputs(RetentionEmailResult.class, eventData);
    }

    @Override // from FieldResult
    protected void doInit (EventData eventData)
    {
        Mailing mailing = new Mailing();
        mailing.init(eventData);
        sent.put(mailing.memberId, mailing);
    }
}
