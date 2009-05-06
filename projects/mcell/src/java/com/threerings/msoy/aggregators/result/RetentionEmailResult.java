//
// $Id$

package com.threerings.msoy.aggregators.result;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.threerings.panopticon.aggregator.result.StringInputNameResult;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;

/**
 * Counts up all the members who were sent a retention email and outputs a map of the member id
 * to the date the email was sent.
 */
@StringInputNameResult(inputs="RetentionMailSent") // original msoy event
public class RetentionEmailResult extends FieldAggregatedResult
{
    /** The members addressed, mapped by bucket, then subject line. */
    public Map<String, Map<String, Set<Integer>>> sent = Maps.newHashMap();

    public static boolean checkInputs (EventData eventData)
    {
        return checkInputs(RetentionEmailResult.class, eventData);
    }

    @Override // from FieldResult
    protected void doInit (EventData eventData)
    {
        String bucket = eventData.getDefaultString("bucket", "default");
        String subject = eventData.getDefaultString("subjectLine", "default");

        Map<String, Set<Integer>> subjectLines = sent.get(bucket);
        if (subjectLines == null) {
            sent.put(bucket, subjectLines = Maps.newHashMap());
        }
        Set<Integer> memberIds = subjectLines.get(subject);
        if (memberIds == null) {
            subjectLines.put(subject, memberIds = Sets.newHashSet());
        }
        memberIds.add(eventData.getInt("recipientId"));
    }
}
