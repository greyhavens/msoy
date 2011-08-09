//
// $Id$

package client.person;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;
import com.threerings.msoy.web.gwt.Activity;

/**
 * Common code for displaying news feeds.
 */
public class FeedUtil
{
    /**
     * Sorts and aggregates a list of activities.
     * @return The timestamp of the earliest recorded message.
     */
    public static long aggregate (List<Activity> activities, List<Activity> result)
    {
        List<FeedMessage> messages = Lists.newArrayList();
        long earliest = Long.MAX_VALUE;

        // Aggregate continuous sections of feed messages
        for (Activity activity : activities) {
            if (activity instanceof FeedMessage) {
                messages.add((FeedMessage) activity);
                earliest = Math.min(earliest, activity.startedAt());
            } else {
                flush(result, messages);
                result.add(activity);
            }
        }
        flush(result, messages);

        return earliest;
    }

    protected static void flush (List<Activity> activities, List<FeedMessage> messages)
    {
        if (!messages.isEmpty()) {
            activities.addAll(FeedMessageAggregator.aggregate(messages, false));
            messages.clear();
        }
    }
}
