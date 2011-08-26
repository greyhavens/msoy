//
// $Id$

package com.threerings.msoy.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Contains information on the current location of a member on a peer.
 */
@com.threerings.util.ActionScript(omit=true)
public class MemberLocation extends SimpleStreamableObject
{
    /** The id of the member represented by this location. */
    public int memberId;

    /** The id of the scene occupied by this member or 0. */
    public int sceneId;

    /** The id of the game or game lobby occupied by this member or 0. */
    public int gameId;

    /** Whether or not the member's game is AVR. */
    public boolean avrGame;
}
