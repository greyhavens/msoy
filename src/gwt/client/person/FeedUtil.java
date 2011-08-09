//
// $Id$

package client.person;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

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
        // Sort a copy
        activities = Lists.newArrayList(activities);
        Collections.sort(activities, MOST_RECENT_FIRST);

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

    protected static final Comparator<Activity> MOST_RECENT_FIRST = new Comparator<Activity>() {
        public int compare (Activity a1, Activity a2) {
            return Longs.compare(a2.startedAt(), a1.startedAt());
        }
    };
}
