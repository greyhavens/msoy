//
// $Id$

package com.threerings.msoy.data;

import java.util.Date;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.util.ActionScript;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.Group;

/**
 * Represents all the data returned for a neighborhood query: an array of {@link NeighborGroup}
 * objects corresponding to the groups of which the member is a member, and an array of
 * {@link NeighborMember} objects corresponding to their friends.
 */
@ActionScript(omit=true)
public class Neighborhood
    implements IsSerializable, Streamable, Cloneable
{
    /**
     * Common members between Neighbourhood Groups and Members.
      */
    public static class NeighborEntity
        implements IsSerializable, Streamable, Cloneable
    {
        /** This entity's home scene ID. */
        public int homeSceneId;

        /** The names of the first few members present in this place. */
        public Set<MemberName> popSet;

        /** How many people are currently logged into this entity's home scene. */
        public int popCount;
    }

    /**
     * Represents data for a {@link Group} in a neighborhood query result. Only 'group' is
     * required by the visualization engine.
     */
    public static class NeighborGroup extends NeighborEntity
    {
        /** The group's id/name. */
        public GroupName group;

        /** The number of members in this group. */
        public int members;

        /** The media description of this group's logo. */
        public MediaDesc logo;

        /** Constructor for unserializing. */
        public NeighborGroup () {
            super();
        }
    }

    /**
     * Represents data for a single member in a neighborhood query result. Only 'member' is
     * required by the visualization engine.
     */
    public static class NeighborMember extends NeighborEntity
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

    /** The member around whom this query is centered, or null. */
    public NeighborMember member;

    /** The group around which this query is centered, or null. */
    public NeighborGroup group;

    /** An array of {@link NeighborGroup} objects for the member's memberships. */
    public NeighborGroup[] neighborGroups;

    /** The friends of the member. */
    public NeighborMember[] neighborMembers;

    /** Constructor for unserializing. */
    public Neighborhood ()
    {
        super();
    }
}
