//
// $Id$

package com.threerings.msoy.room.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.util.Name;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.GroupName;

/**
 * Contains detailed information on a particular room.
 */
public class RoomDetail
    implements IsSerializable
{
    /** More metadata for this room. */
    public RoomInfo info;

    /** The room's full-size snapshot. */
    public MediaDesc snapshot;

    /** The owner of this room (either a MemberCard or a GroupName). */
    public IsSerializable owner;

    /** Whether or not this player may manage this room (i.e. the room is owned by them,
     * or it's owned by a group they manage). */
    public boolean mayManage;

    /** The theme this room is associated with, or null. */
    public GroupName theme;

    /** If this room is themed, whether or not it is a home room template. */
    public boolean isTemplate;

    /** The rating assigned to the room by this player. */
    public byte memberRating;

    /** The number of players who have rated this room. */
    public int ratingCount;
}
