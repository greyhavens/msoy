//
// $Id$

package com.threerings.msoy.peer.data;

/**
 * Contains information on a member in a scene.
 */
public class SceneLocation extends MemberLocation
{
    /** The id of the scene occupied by this member. */
    public int sceneId;

    /** The id of the game whose lobby this member is viewing, or 0. */
    public int lobbyGameId;
}
