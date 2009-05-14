//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;

/**
 * Contains summary information for a game being displayed on the arcade page.
 */
public class GameInfo
    implements IsSerializable
{
    /** Default sort by rating */
    public static final byte SORT_BY_RATING = 0;

    /** Alternate sort by newest */
    public static final byte SORT_BY_NEWEST = 1;

    /** Alternate sort by name */
    public static final byte SORT_BY_NAME = 2;

    /** Alternate sort by category */
    public static final byte SORT_BY_GENRE = 3;

    /** Alternate sort by # people playing */
    public static final byte SORT_BY_PLAYERS_ONLINE = 4;

    /** The maximum allowed length for game names. */
    public static final int MAX_NAME_LENGTH = 64;

    /** The maximum allowed length for game descriptions. */
    public static final int MAX_DESCRIPTION_LENGTH = 200;

    /** Value of {@link #groupId} when there is no associated group. */
    public static final int NO_GROUP = 0;

    /** Identifies the game splash media. */
    public static final String SPLASH_MEDIA = "splash";

    /**
     * Obtains the "development" version of the supplied gameId. This is used as the suiteId for
     * original game sub-items and in some other places.
     */
    public static int toDevId (int gameId)
    {
        return -Math.abs(gameId);
    }

    /** The id of the game in question. */
    public int gameId;

    /** The name of the game in question. */
    public String name;

    /** This game's genre. */
    public byte genre;

    /** True if this is an AVRG, false if it's a parlor game. */
    public boolean isAVRG;

    /** The name of the creator of this game. */
    public MemberName creator;

    /** A more detailed description of the game. */
    public String description;

    /** The game's thumbnail media (will never be null). */
    public MediaDesc thumbMedia;

    /** The game screenshot media (will never be null). */
    public MediaDesc shotMedia;

    /** Optional group associated with this game; 0 means no group */
    public int groupId;

    /** The tag used to identify items in this game's shop. */
    public String shopTag;

    /** The current rating of this item, either 0 or between 1 and 5. */
    public float rating;

    /** The number of user ratings that went into the average rating. */
    public int ratingCount;

    /** Whether or not we believe that this game is integrated with the Whirled API. */
    public boolean integrated;

    /** The number of players currently playing this game. */
    public int playersOnline;

    /**
     * Returns true if the specified member is the creator of this game.
     */
    public boolean isCreator (int memberId)
    {
        return creator.getMemberId() == memberId;
    }
}
