//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.peer.data.CrowdNodeObject;

/**
 * Maintains information on an MSOY peer server.
 */
public class MsoyNodeObject extends CrowdNodeObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>hostedScenes</code> field. */
    public static final String HOSTED_SCENES = "hostedScenes";

    /** The field name of the <code>hostedGames</code> field. */
    public static final String HOSTED_GAMES = "hostedGames";

    /** The field name of the <code>memberLocs</code> field. */
    public static final String MEMBER_LOCS = "memberLocs";
    // AUTO-GENERATED: FIELDS END

    /** Contains info on all scenes hosted by this server. */
    public DSet<HostedPlace> hostedScenes = new DSet<HostedPlace>();

    /** Contains info on all games hosted by this server. */
    public DSet<HostedPlace> hostedGames = new DSet<HostedPlace>();

    /** Contains the current location of all members on this server. */
    public DSet<MemberLocation> memberLocs = new DSet<MemberLocation>();

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>hostedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToHostedScenes (HostedPlace elem)
    {
        requestEntryAdd(HOSTED_SCENES, hostedScenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedScenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromHostedScenes (Comparable key)
    {
        requestEntryRemove(HOSTED_SCENES, hostedScenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateHostedScenes (HostedPlace elem)
    {
        requestEntryUpdate(HOSTED_SCENES, hostedScenes, elem);
    }

    /**
     * Requests that the <code>hostedScenes</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setHostedScenes (DSet<com.threerings.msoy.peer.data.HostedPlace> value)
    {
        requestAttributeChange(HOSTED_SCENES, value, this.hostedScenes);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.peer.data.HostedPlace> clone =
            (value == null) ? null : value.typedClone();
        this.hostedScenes = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>hostedGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToHostedGames (HostedPlace elem)
    {
        requestEntryAdd(HOSTED_GAMES, hostedGames, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedGames</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromHostedGames (Comparable key)
    {
        requestEntryRemove(HOSTED_GAMES, hostedGames, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateHostedGames (HostedPlace elem)
    {
        requestEntryUpdate(HOSTED_GAMES, hostedGames, elem);
    }

    /**
     * Requests that the <code>hostedGames</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setHostedGames (DSet<com.threerings.msoy.peer.data.HostedPlace> value)
    {
        requestAttributeChange(HOSTED_GAMES, value, this.hostedGames);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.peer.data.HostedPlace> clone =
            (value == null) ? null : value.typedClone();
        this.hostedGames = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>memberLocs</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMemberLocs (MemberLocation elem)
    {
        requestEntryAdd(MEMBER_LOCS, memberLocs, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memberLocs</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMemberLocs (Comparable key)
    {
        requestEntryRemove(MEMBER_LOCS, memberLocs, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memberLocs</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMemberLocs (MemberLocation elem)
    {
        requestEntryUpdate(MEMBER_LOCS, memberLocs, elem);
    }

    /**
     * Requests that the <code>memberLocs</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMemberLocs (DSet<com.threerings.msoy.peer.data.MemberLocation> value)
    {
        requestAttributeChange(MEMBER_LOCS, value, this.memberLocs);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.peer.data.MemberLocation> clone =
            (value == null) ? null : value.typedClone();
        this.memberLocs = clone;
    }
    // AUTO-GENERATED: METHODS END
}
