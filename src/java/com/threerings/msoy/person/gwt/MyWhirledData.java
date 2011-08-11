//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.gwt.util.ExpanderResult;

import com.threerings.msoy.web.gwt.Activity;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Promotion;

/**
 * Contains the data that we need for the My Whirled views.
 */
public class MyWhirledData
    implements IsSerializable
{
    /** Hold a list of feed messages of a particular category. */
    public static class FeedCategory
        implements IsSerializable
    {
        /** How many feed messages to list by default in each category */
        public static final int DEFAULT_COUNT = 3;

        /** How many feed messages to list in a category when "show more" is clicked */
        public static final int FULL_COUNT = 50;

        /** The category of the feed messages. */
        public FeedMessageType.Category category;

        /** The highlighted games in this genre. */
        public FeedMessage[] messages;
    }

    /** The total number of people online. */
    public int whirledPopulation;

    /** This member's total friend count (on and offline). */
    public int friendCount;

    /** Promotions to display on the My Whirled page. */
    public List<Promotion> promos;

    /**
     * This member's online friends.
     */
    public List<MemberCard> friends;

    /**
     * Online greeters.
     */
    public List<MemberCard> greeters;

    /**
     * The number of threads in this member's groups with unread posts.
     */
    public int updatedThreads;

    /**
     * News stream activity.
     */
    public ExpanderResult<Activity> stream;
}
