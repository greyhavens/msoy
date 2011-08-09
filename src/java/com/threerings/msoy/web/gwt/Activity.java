//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.Comparator;

import com.google.common.primitives.Longs;
import com.google.gwt.user.client.rpc.IsSerializable;

// A marker interface to tag anything that can show up in someone's feed
public interface Activity
    extends IsSerializable
{
    /** When this activity happened. */
    long startedAt ();

    public static final Comparator<Activity> MOST_RECENT_FIRST = new Comparator<Activity>() {
        public int compare (Activity a1, Activity a2) {
            return Longs.compare(a2.startedAt(), a1.startedAt());
        }
    };
}
