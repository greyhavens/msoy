//
// $Id$

package com.threerings.msoy.person.data.all;

import com.threerings.msoy.data.all.MemberName;

/**
 * Contains data for a friend-originated feed message.
 */
public class FriendFeedMessage extends FeedMessage
{
    /** The name of the friend to whom this message pertains. */
    public MemberName friend;
}
