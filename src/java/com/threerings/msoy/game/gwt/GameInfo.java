//
// $Id$

package com.threerings.msoy.game.gwt;

import java.util.Comparator;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;

/**
 * Contains summary information for a game being displayed on the arcade page.
 */
public class GameInfo
    implements IsSerializable
{
    /** Sort options for games. */
    public enum Sort {
        BY_RATING(new Comparator<GameInfo>() {
            public int compare (GameInfo c1, GameInfo c2) {
                if (c2.rating == c1.rating) {
                    return c2.ratingCount - c1.ratingCount;
                } else {
                    return ((c2.rating > c1.rating) ? 1 : -1);
                }
            }
        }),
        BY_NEWEST(new Comparator<GameInfo>() {
            public int compare (GameInfo c1, GameInfo c2) {
                return c2.gameId - c1.gameId;
            }
        }),
        BY_NAME(new Comparator<GameInfo>() {
            public int compare (GameInfo c1, GameInfo c2) {
                return c1.name.toString().toLowerCase().compareTo(c2.name.toString().toLowerCase());
            }
        }),
        BY_GENRE(new Comparator<GameInfo>() {
            public int compare (GameInfo c1, GameInfo c2) {
                return c2.genre - c1.genre;
            }
        }),
        BY_ONLINE(new Comparator<GameInfo>() {
            public int compare (GameInfo c1, GameInfo c2) {
                return c2.playersOnline - c1.playersOnline;
            }
        });

        public static Sort fromToken (String token) {
            try {
                return Sort.valueOf("BY_" + token.toUpperCase());
            } catch (Exception e) {
                return BY_RATING;
            }
        }

        /** Returns a comparator that sorts according to this option. */
        public final Comparator<GameInfo> comparator;

        public String toToken () {
            return toString().substring(3).toLowerCase();
        }

        Sort (Comparator<GameInfo> comp) {
            this.comparator = comp;
        }
    };

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
