//
// $Id$

package com.threerings.msoy.facebook.gwt;

import com.threerings.orth.data.MediaDesc;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information to be shown in the Facebook friends bar for a single friend.
 */
public class FacebookFriendInfo
    implements IsSerializable
{
    /**
     * A simple thumbnail for a game or trophy.
     */
    public static class Thumbnail
        implements IsSerializable
    {
        /** The id of the game. */
        public int id;

        /** The name of the game. */
        public String name;

        /** The media for the thumbnail. */
        public MediaDesc media;
    }

    /** The member id. */
    public int memberId;

    /** The facebook user id. */
    public long facebookUid;

    /** The Whirled level of the friend. */
    public int level;

    /** The last game played by the friend. */
    public Thumbnail lastGame;

    /** Number of trophies earned in the last game. */
    public int trophyCount;
}
