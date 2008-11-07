//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Contains the data that we need for the My Whirled views.
 */
public class MyWhirledData
    implements IsSerializable
{
    /** Contains summary information on a particular game genre. */
    public static class FeedCategory
        implements IsSerializable
    {
        /** How many feed messages to list by default in each category */
        public static final int DEFAULT_COUNT = 3;

        /** How many feed messages to list in a category when "show more" is clicked */
        public static final int FULL_COUNT = 50;

        /** The category of feed item - see FeedMessageType.Category. */
        public int category;

        /** The highlighted games in this genre. */
        public FeedMessage[] messages;
    }

    /** The total number of people online. */
    public int whirledPopulation;

    /** This member's total friend count (on and offline). */
    public int friendCount;

    /**
     * This member's online friends.
     */
    public List<MemberCard> friends;

    /**
     * This member's recent feed messages broken up by category.
     */
    public List<FeedCategory> feed;
}
