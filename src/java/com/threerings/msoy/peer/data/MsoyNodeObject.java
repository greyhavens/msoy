//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.presents.dobj.DSet;
import com.threerings.crowd.peer.data.CrowdNodeObject;

import com.threerings.msoy.admin.data.PeerAdminMarshaller;
import com.threerings.msoy.party.data.MemberParty;
import com.threerings.msoy.party.data.PartyInfo;
import com.threerings.msoy.party.data.PartySummary;
import com.threerings.msoy.party.data.PeerPartyMarshaller;

/**
 * Maintains information on an MSOY peer server.
 */
public class MsoyNodeObject extends CrowdNodeObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberScenes</code> field. */
    public static final String MEMBER_SCENES = "memberScenes";

    /** The field name of the <code>memberGames</code> field. */
    public static final String MEMBER_GAMES = "memberGames";

    /** The field name of the <code>hostedScenes</code> field. */
    public static final String HOSTED_SCENES = "hostedScenes";

    /** The field name of the <code>hostedGames</code> field. */
    public static final String HOSTED_GAMES = "hostedGames";

    /** The field name of the <code>hostedParties</code> field. */
    public static final String HOSTED_PARTIES = "hostedParties";

    /** The field name of the <code>partyInfos</code> field. */
    public static final String PARTY_INFOS = "partyInfos";

    /** The field name of the <code>memberParties</code> field. */
    public static final String MEMBER_PARTIES = "memberParties";

    /** The field name of the <code>msoyPeerService</code> field. */
    public static final String MSOY_PEER_SERVICE = "msoyPeerService";

    /** The field name of the <code>peerAdminService</code> field. */
    public static final String PEER_ADMIN_SERVICE = "peerAdminService";

    /** The field name of the <code>peerPartyService</code> field. */
    public static final String PEER_PARTY_SERVICE = "peerPartyService";
    // AUTO-GENERATED: FIELDS END

    /** Contains info on all members in a scene on this server. */
    public DSet<MemberScene> memberScenes = DSet.newDSet();

    /** Contains info on all members in a game on this server. */
    public DSet<MemberGame> memberGames = DSet.newDSet();

    /** Contains info on all scenes hosted by this server. */
    public DSet<HostedRoom> hostedScenes = DSet.newDSet();

    /** Contains info on all games hosted by this server. */
    public DSet<HostedGame> hostedGames = DSet.newDSet();

    /** Contains the immutable summaries for all parties on this node. */
    public DSet<PartySummary> hostedParties = DSet.newDSet();

    /** Contains the mutable attributes of a party. */
    public DSet<PartyInfo> partyInfos = DSet.newDSet();

    /** Contains the current partyId of all members partying on this server. */
    public DSet<MemberParty> memberParties = DSet.newDSet();

    /** Handles special communication between MSOY peers. */
    public MsoyPeerMarshaller msoyPeerService;

    /** Admin related peer services. */
    public PeerAdminMarshaller peerAdminService;

    /** Handles party communication between peers. */
    public PeerPartyMarshaller peerPartyService;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>memberScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMemberScenes (MemberScene elem)
    {
        requestEntryAdd(MEMBER_SCENES, memberScenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memberScenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMemberScenes (Comparable<?> key)
    {
        requestEntryRemove(MEMBER_SCENES, memberScenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memberScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMemberScenes (MemberScene elem)
    {
        requestEntryUpdate(MEMBER_SCENES, memberScenes, elem);
    }

    /**
     * Requests that the <code>memberScenes</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMemberScenes (DSet<MemberScene> value)
    {
        requestAttributeChange(MEMBER_SCENES, value, this.memberScenes);
        DSet<MemberScene> clone = (value == null) ? null : value.typedClone();
        this.memberScenes = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>memberGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMemberGames (MemberGame elem)
    {
        requestEntryAdd(MEMBER_GAMES, memberGames, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memberGames</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMemberGames (Comparable<?> key)
    {
        requestEntryRemove(MEMBER_GAMES, memberGames, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memberGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMemberGames (MemberGame elem)
    {
        requestEntryUpdate(MEMBER_GAMES, memberGames, elem);
    }

    /**
     * Requests that the <code>memberGames</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMemberGames (DSet<MemberGame> value)
    {
        requestAttributeChange(MEMBER_GAMES, value, this.memberGames);
        DSet<MemberGame> clone = (value == null) ? null : value.typedClone();
        this.memberGames = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>hostedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToHostedScenes (HostedRoom elem)
    {
        requestEntryAdd(HOSTED_SCENES, hostedScenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedScenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromHostedScenes (Comparable<?> key)
    {
        requestEntryRemove(HOSTED_SCENES, hostedScenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateHostedScenes (HostedRoom elem)
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
    public void setHostedScenes (DSet<HostedRoom> value)
    {
        requestAttributeChange(HOSTED_SCENES, value, this.hostedScenes);
        DSet<HostedRoom> clone = (value == null) ? null : value.typedClone();
        this.hostedScenes = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>hostedGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToHostedGames (HostedGame elem)
    {
        requestEntryAdd(HOSTED_GAMES, hostedGames, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedGames</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromHostedGames (Comparable<?> key)
    {
        requestEntryRemove(HOSTED_GAMES, hostedGames, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateHostedGames (HostedGame elem)
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
    public void setHostedGames (DSet<HostedGame> value)
    {
        requestAttributeChange(HOSTED_GAMES, value, this.hostedGames);
        DSet<HostedGame> clone = (value == null) ? null : value.typedClone();
        this.hostedGames = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>hostedParties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToHostedParties (PartySummary elem)
    {
        requestEntryAdd(HOSTED_PARTIES, hostedParties, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedParties</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromHostedParties (Comparable<?> key)
    {
        requestEntryRemove(HOSTED_PARTIES, hostedParties, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedParties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateHostedParties (PartySummary elem)
    {
        requestEntryUpdate(HOSTED_PARTIES, hostedParties, elem);
    }

    /**
     * Requests that the <code>hostedParties</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setHostedParties (DSet<PartySummary> value)
    {
        requestAttributeChange(HOSTED_PARTIES, value, this.hostedParties);
        DSet<PartySummary> clone = (value == null) ? null : value.typedClone();
        this.hostedParties = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>partyInfos</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPartyInfos (PartyInfo elem)
    {
        requestEntryAdd(PARTY_INFOS, partyInfos, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>partyInfos</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPartyInfos (Comparable<?> key)
    {
        requestEntryRemove(PARTY_INFOS, partyInfos, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>partyInfos</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePartyInfos (PartyInfo elem)
    {
        requestEntryUpdate(PARTY_INFOS, partyInfos, elem);
    }

    /**
     * Requests that the <code>partyInfos</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPartyInfos (DSet<PartyInfo> value)
    {
        requestAttributeChange(PARTY_INFOS, value, this.partyInfos);
        DSet<PartyInfo> clone = (value == null) ? null : value.typedClone();
        this.partyInfos = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>memberParties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMemberParties (MemberParty elem)
    {
        requestEntryAdd(MEMBER_PARTIES, memberParties, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memberParties</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMemberParties (Comparable<?> key)
    {
        requestEntryRemove(MEMBER_PARTIES, memberParties, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memberParties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMemberParties (MemberParty elem)
    {
        requestEntryUpdate(MEMBER_PARTIES, memberParties, elem);
    }

    /**
     * Requests that the <code>memberParties</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMemberParties (DSet<MemberParty> value)
    {
        requestAttributeChange(MEMBER_PARTIES, value, this.memberParties);
        DSet<MemberParty> clone = (value == null) ? null : value.typedClone();
        this.memberParties = clone;
    }

    /**
     * Requests that the <code>msoyPeerService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMsoyPeerService (MsoyPeerMarshaller value)
    {
        MsoyPeerMarshaller ovalue = this.msoyPeerService;
        requestAttributeChange(
            MSOY_PEER_SERVICE, value, ovalue);
        this.msoyPeerService = value;
    }

    /**
     * Requests that the <code>peerAdminService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPeerAdminService (PeerAdminMarshaller value)
    {
        PeerAdminMarshaller ovalue = this.peerAdminService;
        requestAttributeChange(
            PEER_ADMIN_SERVICE, value, ovalue);
        this.peerAdminService = value;
    }

    /**
     * Requests that the <code>peerPartyService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPeerPartyService (PeerPartyMarshaller value)
    {
        PeerPartyMarshaller ovalue = this.peerPartyService;
        requestAttributeChange(
            PEER_PARTY_SERVICE, value, ovalue);
        this.peerPartyService = value;
    }
    // AUTO-GENERATED: METHODS END
}
