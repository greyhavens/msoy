//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Contains information on the current location of a member on a peer.
 */
public class MemberLocation extends SimpleStreamableObject
    implements DSet.Entry
{
    /** Indicates that a member is in a scene. */
    public static final byte SCENE = 1;

    /** Indicates that a member is in a game. */
    public static final byte GAME = 2;

    /** The id of the member represented by this location. */
    public Integer memberId;

    /** Whether this location is a {@link #SCENE} or a {@link #GAME}. */
    public byte type;

    /** The id of the scene or game occupied by this member. */
    public int locationId;

    // from DSet.Entry
    public Comparable getKey ()
    {
        return memberId;
    }
}
