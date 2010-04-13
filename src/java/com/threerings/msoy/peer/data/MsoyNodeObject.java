//
// $Id$

package com.threerings.msoy.peer.data;

import javax.annotation.Generated;
import com.threerings.presents.dobj.DSet;
import com.threerings.crowd.peer.data.CrowdNodeObject;

import com.threerings.msoy.admin.data.PeerAdminMarshaller;
import com.threerings.msoy.game.data.TablesWaiting;
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MEMBER_SCENES = "memberScenes";

    /** The field name of the <code>memberGames</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MEMBER_GAMES = "memberGames";

    /** The field name of the <code>hostedScenes</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String HOSTED_SCENES = "hostedScenes";

    /** The field name of the <code>hostedGames</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String HOSTED_GAMES = "hostedGames";

    /** The field name of the <code>hostedParties</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String HOSTED_PARTIES = "hostedParties";

    /** The field name of the <code>partyInfos</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PARTY_INFOS = "partyInfos";

    /** The field name of the <code>memberParties</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MEMBER_PARTIES = "memberParties";

    /** The field name of the <code>tablesWaiting</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String TABLES_WAITING = "tablesWaiting";

    /** The field name of the <code>msoyPeerService</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MSOY_PEER_SERVICE = "msoyPeerService";

    /** The field name of the <code>peerAdminService</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PEER_ADMIN_SERVICE = "peerAdminService";

    /** The field name of the <code>peerPartyService</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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

    /** Contains gameId/names of games where there are people waiting for other players. */
    public DSet<TablesWaiting> tablesWaiting = DSet.newDSet();

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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToMemberScenes (MemberScene elem)
    {
        requestEntryAdd(MEMBER_SCENES, memberScenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memberScenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromMemberScenes (Comparable<?> key)
    {
        requestEntryRemove(MEMBER_SCENES, memberScenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memberScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setMemberScenes (DSet<MemberScene> value)
    {
        requestAttributeChange(MEMBER_SCENES, value, this.memberScenes);
        DSet<MemberScene> clone = (value == null) ? null : value.clone();
        this.memberScenes = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>memberGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToMemberGames (MemberGame elem)
    {
        requestEntryAdd(MEMBER_GAMES, memberGames, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memberGames</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromMemberGames (Comparable<?> key)
    {
        requestEntryRemove(MEMBER_GAMES, memberGames, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memberGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setMemberGames (DSet<MemberGame> value)
    {
        requestAttributeChange(MEMBER_GAMES, value, this.memberGames);
        DSet<MemberGame> clone = (value == null) ? null : value.clone();
        this.memberGames = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>hostedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToHostedScenes (HostedRoom elem)
    {
        requestEntryAdd(HOSTED_SCENES, hostedScenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedScenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromHostedScenes (Comparable<?> key)
    {
        requestEntryRemove(HOSTED_SCENES, hostedScenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setHostedScenes (DSet<HostedRoom> value)
    {
        requestAttributeChange(HOSTED_SCENES, value, this.hostedScenes);
        DSet<HostedRoom> clone = (value == null) ? null : value.clone();
        this.hostedScenes = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>hostedGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToHostedGames (HostedGame elem)
    {
        requestEntryAdd(HOSTED_GAMES, hostedGames, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedGames</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromHostedGames (Comparable<?> key)
    {
        requestEntryRemove(HOSTED_GAMES, hostedGames, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setHostedGames (DSet<HostedGame> value)
    {
        requestAttributeChange(HOSTED_GAMES, value, this.hostedGames);
        DSet<HostedGame> clone = (value == null) ? null : value.clone();
        this.hostedGames = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>hostedParties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToHostedParties (PartySummary elem)
    {
        requestEntryAdd(HOSTED_PARTIES, hostedParties, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedParties</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromHostedParties (Comparable<?> key)
    {
        requestEntryRemove(HOSTED_PARTIES, hostedParties, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedParties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setHostedParties (DSet<PartySummary> value)
    {
        requestAttributeChange(HOSTED_PARTIES, value, this.hostedParties);
        DSet<PartySummary> clone = (value == null) ? null : value.clone();
        this.hostedParties = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>partyInfos</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToPartyInfos (PartyInfo elem)
    {
        requestEntryAdd(PARTY_INFOS, partyInfos, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>partyInfos</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromPartyInfos (Comparable<?> key)
    {
        requestEntryRemove(PARTY_INFOS, partyInfos, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>partyInfos</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPartyInfos (DSet<PartyInfo> value)
    {
        requestAttributeChange(PARTY_INFOS, value, this.partyInfos);
        DSet<PartyInfo> clone = (value == null) ? null : value.clone();
        this.partyInfos = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>memberParties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToMemberParties (MemberParty elem)
    {
        requestEntryAdd(MEMBER_PARTIES, memberParties, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memberParties</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromMemberParties (Comparable<?> key)
    {
        requestEntryRemove(MEMBER_PARTIES, memberParties, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memberParties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setMemberParties (DSet<MemberParty> value)
    {
        requestAttributeChange(MEMBER_PARTIES, value, this.memberParties);
        DSet<MemberParty> clone = (value == null) ? null : value.clone();
        this.memberParties = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>tablesWaiting</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToTablesWaiting (TablesWaiting elem)
    {
        requestEntryAdd(TABLES_WAITING, tablesWaiting, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>tablesWaiting</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromTablesWaiting (Comparable<?> key)
    {
        requestEntryRemove(TABLES_WAITING, tablesWaiting, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>tablesWaiting</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void updateTablesWaiting (TablesWaiting elem)
    {
        requestEntryUpdate(TABLES_WAITING, tablesWaiting, elem);
    }

    /**
     * Requests that the <code>tablesWaiting</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setTablesWaiting (DSet<TablesWaiting> value)
    {
        requestAttributeChange(TABLES_WAITING, value, this.tablesWaiting);
        DSet<TablesWaiting> clone = (value == null) ? null : value.clone();
        this.tablesWaiting = clone;
    }

    /**
     * Requests that the <code>msoyPeerService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPeerPartyService (PeerPartyMarshaller value)
    {
        PeerPartyMarshaller ovalue = this.peerPartyService;
        requestAttributeChange(
            PEER_PARTY_SERVICE, value, ovalue);
        this.peerPartyService = value;
    }
    // AUTO-GENERATED: METHODS END
}
