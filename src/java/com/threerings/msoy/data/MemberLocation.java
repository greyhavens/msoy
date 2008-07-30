//
// $Id$

package com.threerings.msoy.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Contains information on the current location of a member on a peer.
 */
public class MemberLocation extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The id of the member represented by this location. */
    public Integer memberId;

    /** The id of the scene occupied by this member or 0. */
    public int sceneId;

    /** The id of the game or game lobby occupied by this member. */
    public int gameId;

    /** Whether or not this is an AVRGame. */
    public boolean avrGame;

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return memberId;
    }
}
