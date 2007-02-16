//
// $Id$

package com.threerings.msoy.web.data;

import java.util.Date;

/**
 * Represents data for a single member in a neighborhood query result. Only 'member' is
 * required by the visualization engine.
 */
public class NeighborMember extends NeighborEntity
{
    /** The member's id/name. */
    public MemberName member;

    /** Whether or not this member is currently online. */
    public boolean isOnline;

    /** The quantity of flow possessed by this member. */
    public int flow;

    /** The time at which this player was created. */
    public Date created;

    /** The number of sessions this player has played. */
    public int sessions;

    /** The cumulative number of minutes spent playing. */
    public int sessionMinutes;

    /** The time at which the player ended their last session. */
    public Date lastSession;
}
