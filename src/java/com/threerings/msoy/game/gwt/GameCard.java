//
// $Id$

package com.threerings.msoy.game.gwt;

import java.util.Comparator;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the bare bones info we show about a game on the arcade page.
 */
public class GameCard
    implements IsSerializable
{
    /** A comparator for sorting cards by name. */
    public static final Comparator<GameCard> BY_NAME = new Comparator<GameCard>() {
        public int compare (GameCard one, GameCard two) {
            return one.name.compareTo(two.name);
        }
    };

    /** The id of the game in question. */
    public int gameId;

    /** The name of the game in question. */
    public String name;

    /** The game's thumbnail media (will never be null). */
    public MediaDesc thumbMedia;

    /** The number of players currently playing this game. */
    public int playersOnline;
}
