//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.all.Trophy;

/**
 * Contains information on all trophies owned by a player.
 */
public class TrophyCase implements IsSerializable
{
    /** Contains information on one game's trophies. */
    public static class Shelf implements IsSerializable
    {
        /** The id of the game from which these trophies where earned. */
        public int gameId;

        /** The name of the game from which these trophies were earned. */
        public String name;

        /** The earned trophies, in order of when they were earned. */
        public Trophy[] trophies;
    }

    /** The member that owns these trophies. */
    public MemberName owner;

    /** The shelves of trophies. */
    public Shelf[] shelves;
}
