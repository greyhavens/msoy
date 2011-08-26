//
// $Id$

package com.threerings.msoy.peer.data;

/**
 * Tracks the current game (or game lobby) occupied by a member.
 */
@com.threerings.util.ActionScript(omit=true)
public class MemberGame extends MemberDatum
{
    /** The id of the game (or game lobby) occupied by this member. */
    public int gameId;

    /** Whether or not the member's game is AVR. */
    public boolean avrGame;

    /** Creates a configured instance. */
    public MemberGame (int memberId, int gameId, boolean avrGame)
    {
        this.memberId = memberId;
        this.gameId = gameId;
        this.avrGame = avrGame;
    }

    /** Used when unserializing. */
    public MemberGame ()
    {
    }
}
