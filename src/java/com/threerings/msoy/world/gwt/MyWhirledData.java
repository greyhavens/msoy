//
// $Id$

package com.threerings.msoy.world.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.web.data.MemberCard;

/**
 * Contains the data that we need for the My Whirled views.
 */
public class MyWhirledData
    implements IsSerializable
{
    /** The total number of people online. */
    public int whirledPopulation;

    /** This member's total friend count (on and offline). */
    public int friendCount;

    /**
     * This member's online friends.
     */
    public List<MemberCard> friends;

    /**
     * This member's recent feed messages.
     */
    public List<FeedMessage> feed;
}
