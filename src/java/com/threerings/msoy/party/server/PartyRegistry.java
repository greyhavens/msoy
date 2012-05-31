//
// $Id$

package com.threerings.msoy.party.server;

import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.Invoker;
import com.samskivert.util.QuickSort;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.SessionFactory;
import com.threerings.presents.server.net.PresentsConnectionManager;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.BodyManager;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.whirled.data.ScenePlace;

import com.threerings.msoy.admin.data.CostsConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyUserObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.server.PlayerLocator;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.gwt.CostUpdatedException;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.notify.data.PartyInviteNotification;
import com.threerings.msoy.notify.server.MsoyNotificationManager;
import com.threerings.msoy.party.client.PartyBoardService;
import com.threerings.msoy.party.data.PartyAuthName;
import com.threerings.msoy.party.data.PartyBoardInfo;
import com.threerings.msoy.party.data.PartyBoardMarshaller;
import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyCredentials;
import com.threerings.msoy.party.data.PartyInfo;
import com.threerings.msoy.party.data.PartyLeader;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyOccupantInfo;
import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;
import com.threerings.msoy.party.data.PeerPartyMarshaller;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.util.ServiceUnit;

import static com.threerings.msoy.Log.log;

/**
 * The PartyRegistry manages all the PartyManagers on a single node. It handles PartyBoard
 * requests coming from a user's world connection. Once a user is in a party, they talk
 * to their PartyManager via their party connection.
 */
@Singleton
public class PartyRegistry
    implements PartyBoardProvider, PeerPartyProvider
{
    @Inject public PartyRegistry (InvocationManager invmgr, PresentsConnectionManager conmgr,
                                  ClientManager clmgr, PartyAuthenticator partyAuthor)
    {
        invmgr.registerProvider(this, PartyBoardMarshaller.class, MsoyCodes.WORLD_GROUP);
        partyAuthor.init(this); // fiddling to work around a circular dependency
        conmgr.addChainedAuthenticator(partyAuthor);
        clmgr.addSessionFactory(SessionFactory.newSessionFactory(
                                    PartyCredentials.class, PartySession.class,
                                    PartyAuthName.class, PartyClientResolver.class));
    }

    /**
     * Called to initialize the PartyRegistry after server startup.
     */
    public void init ()
    {
        _peerMgr.getMsoyNodeObject().setPeerPartyService(
            _invmgr.registerProvider(this, PeerPartyMarshaller.class));
    }

    /**
     * Return the size of the specified user's party, or 0 if they're not in a party.
     */
    public int lookupPartyPopulation (MsoyUserObject user)
    {
        PartySummary party = user.getParty();
        return (party == null) ? 0 : lookupPartyPopulation(party.id);
    }

    /**
     * Can be called to return the current size of any party, even one not hosted on this node.
     */
    public int lookupPartyPopulation (int partyId)
    {
        PartyInfo info = lookupPartyInfo(partyId);
        return (info == null) ? 0 : info.population;
    }

    /**
     * Returns the manager for the specified party or null.
     */
    public PartyManager getPartyManager (int partyId)
    {
        return _parties.get(partyId);
    }

    /**
     * Called on the server that hosts the passed-in player, not necessarily on the server
     * hosting the party.
     */
    public void issueInvite (MemberObject member, MemberName inviter, int partyId, String partyName)
    {
        _notifyMan.notify(member, new PartyInviteNotification(inviter, partyId, partyName));
    }

    /**
     * Called by a PartyPlaceManager when a user enters.
     */
    public void userEnteringPlace (MsoyUserObject userObj, PartyPlaceObject placeObj)
    {
        PartySummary summary = userObj.getParty();
        if ((summary != null) && !placeObj.getParties().containsKey(summary.id)) {
            // look up the leader and add that
            placeObj.addToPartyLeaders(new PartyLeader(summary.id, lookupLeaderId(summary.id)));
            placeObj.addToParties(summary);
            _partyPlaces.put(summary.id, placeObj);
        }
    }

    /**
     * Called by a PartyPlaceManager when a user enters.
     */
    public void userLeavingPlace (MsoyUserObject userObj, PartyPlaceObject placeObj)
    {
        maybeRemovePartyFromPlace(userObj.getParty(), placeObj);
    }

    /**
     * Called when a user's party id changes. Happens in two places:
     * - from PartyManager, when the party is hosted on this node.
     * - from MsoyPeerNode, for parties hosted on other nodes.
     */
    public void updateUserParty (int memberId, int partyId, MsoyNodeObject nodeObj)
    {
        MsoyUserObject memberObj = _memberLocator.lookupMember(memberId);
        MsoyUserObject playerObj = _playerLocator.lookupPlayer(memberId);
        if (memberObj == null && playerObj == null) {
            return; // this node officially doesn't care
        }

        // we know that the PartySummary for this party is on the same nodeObj
        PartySummary summary = (partyId == 0) ? null : nodeObj.hostedParties.get(partyId);
        if (memberObj != null) {
            updateUserParty(memberObj, summary);
        }
        if (playerObj != null) {
            updateUserParty(playerObj, summary);
        }
    }

    /**
     * Called when a PartyInfo changes. Happens in two places:
     * - from PartyManager, when the info is published.
     * - from MsoyPeerNode, when it detects an info change.
     */
    public void partyInfoChanged (PartyInfo oldInfo, PartyInfo newInfo)
    {
        if (oldInfo.leaderId == newInfo.leaderId) {
            return;
        }

        // publish a new leader id to all the places currently hosting this party
        PartyLeader leader = new PartyLeader(newInfo.id, newInfo.leaderId);
        for (PartyPlaceObject placeObj : _partyPlaces.get(newInfo.id)) {
            placeObj.updatePartyLeaders(leader);
        }
    }

    /**
     * Returns the group id of the specified party or 0 if the party does not exist.
     */
    public int getPartyGroupId (int partyId)
    {
        PartyManager mgr = _parties.get(partyId);
        return (mgr == null) ? 0 : mgr.getPartyObject().group.getGroupId();
    }

    /**
     * Requests that the supplied member pre-join the specified party. If the method returns
     * normally, the player will have been added to the specified party.
     *
     * @throws InvocationException if the party cannot be joined for some reason.
     */
    public void preJoinParty (MemberName name, int partyId, Rank rank)
        throws InvocationException
    {
        PartyManager mgr = _parties.get(partyId);
        if (mgr == null) {
            throw new InvocationException(PartyCodes.E_NO_SUCH_PARTY);
        }
        mgr.addPlayer(name, rank);
    }

    // from PartyBoardProvider
    public void locateParty (ClientObject co, final int partyId, PartyBoardService.JoinListener jl)
        throws InvocationException
    {
        String pnode = _peerMgr.lookupNodeDatum(new Function<NodeObject, String>() {
            public String apply (NodeObject nobj) {
                return ((MsoyNodeObject)nobj).hostedParties.containsKey(partyId)
                    ? nobj.nodeName : null;
            }
        });
        if (pnode == null) {
            throw new InvocationException(PartyCodes.E_NO_SUCH_PARTY);
        }
        jl.foundParty(partyId, _peerMgr.getPeerPublicHostName(pnode), _peerMgr.getPeerPort(pnode));
    }

    // from PartyBoardProvider
    public void getPartyBoard (
        ClientObject caller, final byte mode, final InvocationService.ResultListener rl)
        throws InvocationException
    {
        final MemberObject member = _memberLocator.requireMember(caller);

        final List<PartyBoardInfo> list = Lists.newArrayList();
        for (MsoyNodeObject nodeObj : _peerMgr.getMsoyNodeObjects()) {
            for (PartyInfo info : nodeObj.partyInfos) {
                if ((info.population >= PartyCodes.MAX_PARTY_SIZE) ||
                    (info.recruitment == PartyCodes.RECRUITMENT_CLOSED)) {
                    continue; // skip: too big, or closed
                }
                if ((mode == PartyCodes.BOARD_AWAITING_PLAYERS) &&
                    (info.statusType != PartyCodes.STATUS_TYPE_LOBBY)) {
                    continue; // skip: we want only boards awaiting players.
                }
                PartySummary summary = nodeObj.hostedParties.get(info.id);
                if ((info.recruitment == PartyCodes.RECRUITMENT_GROUP) &&
                    !member.isGroupMember(summary.group.getGroupId())) {
                    continue; // skip: user not a group member
                }
                PartyBoardInfo boardInfo = new PartyBoardInfo(summary, info);
                boardInfo.computeScore(member);
                list.add(boardInfo);
            }
        }

        // sort and prune
        // Note: perhaps create a data structure that only saves the top N items and rolls
        // the rest off.
        QuickSort.sort(list);
        CollectionUtil.limit(list, PARTIES_PER_BOARD);
        rl.requestProcessed(list);
    }

    // from PartyBoardProvider
    public void getCreateCost (ClientObject caller, InvocationService.ResultListener rl)
        throws InvocationException
    {
        final MemberObject member = _memberLocator.requireMember(caller);

        PriceQuote quote = _moneyLogic.securePrice(
            member.getMemberId(), PARTY_PURCHASE_KEY, Currency.COINS, getPartyCoinCost());
        rl.requestProcessed(quote);
    }

    // from PartyBoardProvider
    public void createParty (
        ClientObject caller, final Currency currency, final int authedAmount,
        final String name, final int groupId, final boolean inviteAllFriends,
        final PartyBoardService.JoinListener jl)
        throws InvocationException
    {
        final MemberObject member = _memberLocator.requireMember(caller);

        if (member.partyId != 0) {
            // TODO: possibly a better error? Surely this will be blocked on the client
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
        }
        // verify that the user is at least a member of the specified group
        final GroupMembership groupInfo = member.groups.get(groupId);
        if (groupInfo == null) {
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR); // shouldn't happen
        }

        final int cost = getPartyCoinCost();
        _invoker.postUnit(new ServiceUnit("createParty", jl) {
            public void invokePersistent () throws Exception {
                _group = _groupRepo.loadGroup(groupId);
                if ((_group == null) ||
                        ((_group.partyPerms == Group.Perm.MANAGER) &&
                        (groupInfo.rank.compareTo(Rank.MANAGER) < 0))) {
                    throw new InvocationException(PartyCodes.E_GROUP_MGR_REQUIRED);
                }
                if (cost != 0) {
                    _moneyLogic.buyParty(member.getMemberId(), PARTY_PURCHASE_KEY,
                        currency, authedAmount, Currency.COINS, cost);
                }
            }
            @Override public void handleFailure (Exception error) {
                if (error instanceof CostUpdatedException) {
                    jl.priceUpdated(((CostUpdatedException) error).getQuote());
                } else {
                    super.handleFailure(error);
                }
            }
            public void handleSuccess () {
                finishCreateParty(member, name, _group, groupInfo, inviteAllFriends, jl);
            }
            protected GroupRecord _group;
        });
    }

    // from PartyBoardProvider & PeerPartyProvider
    public void getPartyDetail (ClientObject caller, final int partyId,
                                final InvocationService.ResultListener rl)
        throws InvocationException
    {
        // see if we can handle it locally
        PartyManager mgr = _parties.get(partyId);
        if (mgr != null) {
            rl.requestProcessed(mgr.getPartyDetail());
            return;
        }

        // otherwise ship it off to the node that handles it
        int sent = _peerMgr.invokeOnNodes(new Function<Tuple<Client,NodeObject>,Boolean>() {
            public Boolean apply (Tuple<Client,NodeObject> clinode) {
                MsoyNodeObject mnode = (MsoyNodeObject)clinode.right;
                if (!mnode.hostedParties.containsKey(partyId)) {
                    return false;
                }
                mnode.peerPartyService.getPartyDetail(partyId, rl);
                return true;
            }
        });
        if (sent == 0) {
            throw new InvocationException(PartyCodes.E_NO_SUCH_PARTY);
        }
    }

    /**
     * Called by a PartyManager when it's removed.
     */
    void partyWasRemoved (int partyId)
    {
        _parties.remove(partyId);
    }

    /**
     * Finish creating a new party.
     */
    protected void finishCreateParty (MemberObject member, String name, GroupRecord group,
                                      GroupMembership groupInfo, boolean inviteAllFriends,
                                      PartyBoardService.JoinListener jl)
    {
        PartyObject pobj = null;
        PartyManager mgr = null;
        try {
            // set up the new PartyObject
            pobj = _omgr.registerObject(new PartyObject());
            pobj.id = _peerMgr.getNextPartyId();
            pobj.name = StringUtil.truncate(name, PartyCodes.MAX_NAME_LENGTH);
            pobj.group = groupInfo.group;
            pobj.icon = group.toLogo();
            pobj.leaderId = member.getMemberId();
            pobj.disband = true;
            if (member.location instanceof ScenePlace) {
                pobj.sceneId = ((ScenePlace) member.location).sceneId;
            }

            // create the PartyManager and add the member
            mgr = _injector.getInstance(PartyManager.class);
            mgr.init(pobj, member.getMemberId());
            mgr.addPlayer(member.memberName, groupInfo.rank);

            // we're hosting this party so we send them to this same node
            jl.foundParty(pobj.id, ServerConfig.serverHost, ServerConfig.serverPorts[0]);

        } catch (Exception e) {
            log.warning("Problem creating party", e);
            if (e instanceof InvocationException) {
                jl.requestFailed(e.getMessage());
            } else {
                jl.requestFailed(InvocationCodes.E_INTERNAL_ERROR);
            }

            // kill the party object we created
            if (mgr != null) {
                mgr.shutdown();
            }
            if (pobj != null) {
                _omgr.destroyObject(pobj.getOid());
            }
            return;
        }

        // if we made it here, then register the party
        _parties.put(pobj.id, mgr);

        if (inviteAllFriends) {
            mgr.inviteAllFriends(member);
        }
    }

    /**
     * Called when the member represented by the specified user object has joined or left a party.
     */
    protected void updateUserParty (MsoyUserObject userObj, PartySummary party)
    {
        // first update the user
        PartySummary oldSummary = userObj.getParty();
        userObj.setParty(party);

        // then any place they may occupy
        PlaceManager placeMan = _placeReg.getPlaceManager(userObj.getPlaceOid());
        if (placeMan != null) {
            PlaceObject placeObj = placeMan.getPlaceObject();
            if (placeObj instanceof PartyPlaceObject) {
                placeObj.startTransaction();
                try {
                    // we need to add a new party BEFORE updating the occInfo
                    userEnteringPlace(userObj, (PartyPlaceObject)placeObj);
                    // update the occupant info
                    final int newPartyId = (party == null) ? 0 : party.id;
                    placeMan.updateOccupantInfo(userObj.getOid(),
                        new OccupantInfo.Updater<OccupantInfo>() {
                            public boolean update (OccupantInfo info) {
                                return ((PartyOccupantInfo) info).updatePartyId(newPartyId);
                            }
                        });
                    // we need to remove an old party AFTER updating the occInfo
                    maybeRemovePartyFromPlace(oldSummary, (PartyPlaceObject)placeObj);
                } finally {
                    placeObj.commitTransaction();
                }
            }
        }
    }

    /**
     * Figure out the leaderId of the specified party.
     */
    protected int lookupLeaderId (int partyId)
    {
        PartyInfo info = lookupPartyInfo(partyId);
        return (info == null) ? 0 : info.leaderId;
    }

    /**
     * Look up the PartyInfo from the node objects.
     */
    protected PartyInfo lookupPartyInfo (int partyId)
    {
        final Integer partyKey = partyId;
        return _peerMgr.lookupNodeDatum(new Function<NodeObject, PartyInfo>() {
            public PartyInfo apply (NodeObject nobj) {
                return ((MsoyNodeObject)nobj).partyInfos.get(partyKey);
            }
        });
    }

    /**
     * Called when we should remove a party from a place.
     */
    protected void maybeRemovePartyFromPlace (PartySummary summary, PartyPlaceObject placeObj)
    {
        if ((summary == null) || !placeObj.getParties().containsKey(summary.id)) {
            return;
        }
        for (OccupantInfo info : placeObj.getOccupants()) {
            if ((info instanceof PartyOccupantInfo) &&
                    (((PartyOccupantInfo) info).getPartyId() == summary.id)) {
                return; // there's still a partier here!
            }
        }
        placeObj.removeFromParties(summary.id);
        placeObj.removeFromPartyLeaders(summary.id);
        _partyPlaces.remove(summary.id, placeObj);
    }

    protected int getPartyCoinCost ()
    {
        return _runtime.getCoinCost(CostsConfigObject.START_PARTY);
    }

    protected Map<Integer, PartyManager> _parties = Maps.newHashMap();

    protected Multimap<Integer,PartyPlaceObject> _partyPlaces = HashMultimap.create();

    protected static final int PARTIES_PER_BOARD = 10;

    /** Just a unique key. */
    protected static final Object PARTY_PURCHASE_KEY = new Object();

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected BodyManager _bodyMan;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected Injector _injector;
    @Inject protected InvocationManager _invmgr;
    @Inject protected MemberLocator _memberLocator;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyPeerManager _peerMgr;
    @Inject protected MsoyNotificationManager _notifyMan;
    @Inject protected PlaceRegistry _placeReg;
    @Inject protected PlayerLocator _playerLocator;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected RuntimeConfig _runtime;
}
