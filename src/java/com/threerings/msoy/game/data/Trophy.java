//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains information on a trophy held by a player.
 */
public class Trophy extends SimpleStreamableObject
    implements DSet.Entry
{
    /** Used by {@link #getKey}. */
    public class TrophyKey implements Comparable {
        public String ident;
        public int memberId;

        public TrophyKey (String ident, int memberId) {
            this.ident = ident;
            this.memberId = memberId;
        }

        // from Comparable
        public int compareTo (Object other) {
            TrophyKey okey = (TrophyKey)other;
            int rv = okey.ident.compareTo(ident);
            return (rv == 0) ? (okey.memberId - memberId) : rv;
        }

        // @Override // from Object
        public int hashCode () {
            return memberId ^ ident.hashCode();
        }

        // @Override // from Object
        public boolean equals (Object other) {
            return (compareTo((TrophyKey)other) == 0);
        }
    }

    /** The identifier provided by the game for this trophy. */
    public String ident;

    /** The member id of the trophy holder. */
    public int memberId;

    /** The name of the trophy. */
    public String name;

    /** The media for the trophy image. */
    public MediaDesc trophyMedia;

    // from DSet.Entry
    public Comparable getKey ()
    {
        if (_key == null) {
            _key = new TrophyKey(ident, memberId);
        }
        return _key;
    }

    protected transient TrophyKey _key;
}
