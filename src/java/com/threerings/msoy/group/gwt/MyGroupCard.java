//
// $Id$

package com.threerings.msoy.group.gwt;

import com.threerings.msoy.fora.gwt.ForumThread;

/**
 * Detailed information on a single Group/Whirled for the "My Whirleds" page.
 */
public class MyGroupCard
    extends GroupCard
{
    /** Default sort by current population, then by latest thread, then name */
    public static final byte SORT_BY_PEOPLE_ONLINE = 0;
    /** Alternate sort by name */
    public static final byte SORT_BY_NAME = 1;
    /** Default sort by whether I manage, then by population, then latest thread, then name */
    public static final byte SORT_BY_MANAGER = 2;
    /** Alternate sort by latest post, then population, then name */
    public static final byte SORT_BY_NEWEST_POST = 3;

    /** Total number of threads in this group's discussions */
    public int numThreads;

    /** Total number of posts to all threads in this group's discussions */
    public int numPosts;

    /** The member's rank in the group. */
    public byte rank;

    /** Most recent thread for this Whirled on the My Discussions page */
    public ForumThread latestThread;
}
