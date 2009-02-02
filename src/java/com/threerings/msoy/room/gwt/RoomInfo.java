//
// $Id$

package com.threerings.msoy.room.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains brief information on a particular room.
 */
public class RoomInfo
    implements IsSerializable
{
    /** The scene id of this room. */
    public int sceneId;

    /** The name of this room. */
    public String name;

    /** The room's canonical thumbnail snapshot. */
    public MediaDesc thumbnail;

    /** The room's average rating. */
    public float rating;

    /** The number of players in the room. */
    public int population;

    /** Some rooms are winners or runners-up of a contest; if so designate them here. */
    public String winnerRank;
}
