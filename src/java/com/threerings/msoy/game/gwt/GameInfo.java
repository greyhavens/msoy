//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;

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

    /** Alternate sort with [23456]+ player games first */
    public static final byte SORT_BY_MULTIPLAYER = 3;

    /** Alternate sort with 1+ player games first */
    public static final byte SORT_BY_SINGLE_PLAYER = 4;

    /** Alternate sort by category */
    public static final byte SORT_BY_GENRE = 5;

    /** Alternate sort by # people playing */
    public static final byte SORT_BY_PLAYERS_ONLINE = 6;

    /** The unique identifier for this game. */
    public int gameId;

    /** The game's human readable name. */
    public String name;

    /** The genre code for this game. */
    public byte genre;

    /** This game's thumbnail icon. */
    public MediaDesc thumbMedia;

    /** This game's description. */
    public String description;

    /** The number of players currently playing this game. */
    public int playersOnline;

    /** The minimum number of players for this game. */
    public int minPlayers;

    /** The maximum number of players for this game or Integer.MAX_VALUE if it's a party game. */
    public int maxPlayers;

    /** The current rating of this item, either 0 or between 1 and 5. */
    public float rating;

    /** The number of user ratings that went into the average rating. */
    public int ratingCount;

    /** Whether this game takes place in whirled rooms as opposed to being lobbied. */
    public boolean isInWorld;

    /** The group (whirled) associated with this game. */
    public int groupId;
}
