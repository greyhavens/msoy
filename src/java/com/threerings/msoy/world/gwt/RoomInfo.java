//
// $Id$

package com.threerings.msoy.world.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.util.Name;

/**
 * Contains information on a particular room.
 */
public class RoomInfo
    implements IsSerializable
{
    /** The scene id of this room. */
    public int sceneId;

    /** The name of this room. */
    public String name;

    /** The owner of this room (either a MemberName or a GroupName). */
    public Name owner;
}
