//
// $Id$

package com.threerings.msoy.peer.data;

/**
 * Tracks the current scene occupied by a member.
 */
@com.threerings.util.ActionScript(omit=true)
public class MemberScene extends MemberDatum
{
    /** The id of the scene occupied by this member. */
    public int sceneId;

    /** Creates a configured instance. */
    public MemberScene (Integer memberId, int sceneId)
    {
        this.memberId = memberId;
        this.sceneId = sceneId;
    }

    /** Used when unserializing. */
    public MemberScene ()
    {
    }
}
