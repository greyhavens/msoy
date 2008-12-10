//
// $Id$

package com.threerings.msoy.party.server;

import java.util.TreeMap;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.util.Comparators;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Tuple;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.peer.data.NodeObject;

import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.whirled.data.ScenePlace;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.VizMemberName;

import com.threerings.msoy.group.data.all.GroupMembership;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.party.client.PeerPartyService;
import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyInfo;
import com.threerings.msoy.party.data.PartyObject;

import static com.threerings.msoy.Log.log;

@Singleton
public class PartyRegistry
    implements PartyBoardProvider, PeerPartyProvider
{
    @Inject public PartyRegistry (InvocationManager invmgr)
    {
        _invmgr = invmgr;
        invmgr.registerDispatcher(new PartyBoardDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

    /**
     * Called to initialize the PartyRegistry after server startup.
     */
    public void init ()
    {
        ((MsoyNodeObject) _peerMgr.getNodeObject()).setPeerPartyService(
            _invmgr.registerDispatcher(new PeerPartyDispatcher(this)));
    }

    // from PartyBoardProvider
    public void locateMyParty (ClientObject caller, InvocationService.ResultListener rl)
        throws InvocationException
    {
        MemberObject member = (MemberObject)caller;

        if (member.partyId == 0) {
            // TODO: throw no error, or just ignore it on the client
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
        }

        // see if we have the party here
        PartyManager mgr = _parties.get(member.partyId);
        if (mgr != null) {
            rl.requestProcessed(new int[] { mgr.getPartyOid() });
            return;
        }

        Tuple<Client,PeerPartyService> tuple = locatePeerService(member.partyId);
        if (tuple == null) {
            log.warning("Member in party that cannot be found",
                "member", member.who(), "partyId", member.partyId);
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
        }

        tuple.right.getPartyScene(tuple.left, member.partyId, rl);
    }

    // from PartyBoardProvider
    public void getPartyBoard (
        ClientObject caller, String query, InvocationService.ResultListener rl)
        throws InvocationException
    {
        final MemberObject member = (MemberObject)caller;

        // add every party we have access to to a collection sorted by score
        // Note: maybe we use a KeyValue, and create a top-N data structure that only even
        // saves the top-N candidates
        final TreeMap<PartySort,PartyInfo> myParties = Maps.newTreeMap();
        _peerMgr.applyToNodes(new Function<NodeObject,Void>() {
            public Void apply (NodeObject node) {
                for (PartyInfo party : ((MsoyNodeObject) node).parties) {
                    if (party.isVisible(member)) {
                        myParties.put(computePartySort(party, member), party);
                    }
                }
                return null; // Void
            }
        });
        // return a list with maximum PARTIES_PER_BOARD parties
        rl.requestProcessed(
            Lists.newArrayList(Iterables.limit(myParties.values(), PARTIES_PER_BOARD)));
    }

    // from PartyBoardProvider
    public void joinParty (
        ClientObject caller, final int partyId, final InvocationService.ResultListener rl)
        throws InvocationException
    {
        final MemberObject member = (MemberObject)caller;

        // reject them if they're already in a party
        if (member.partyId != 0) {
            throw new InvocationException(InvocationCodes.E_ACCESS_DENIED);
        }

        // figure out their rank in the specified party
        PartyInfo info = _peerMgr.getPartyInfo(partyId);
        if (info == null) {
            throw new InvocationException(PartyCodes.E_NO_SUCH_PARTY);
        }

        // pass the buck
        joinParty(null, partyId, member.memberName, member.getGroupRank(info.group.getGroupId()),
            new InvocationService.ResultListener() {
                public void requestFailed (String cause) {
                    rl.requestFailed(cause);
                }

                public void requestProcessed (Object result) {
                    rl.requestProcessed(result); // send along the sceneId first
                    member.setPartyId(partyId); // then set the partyId.
                }
            });
    }

    // from PeerPartyProvider
    public void joinParty (
        ClientObject caller, int partyId, VizMemberName name, byte groupRank,
        InvocationService.ResultListener rl)
        throws InvocationException
    {
        PartyManager mgr = _parties.get(partyId);
        if (mgr != null) {
            // we can satisfy this request directly!
            mgr.addPlayer(name, groupRank, rl);
            return;
        }

        Tuple<Client,PeerPartyService> tuple = locatePeerService(partyId);
        if (tuple == null) {
            throw new InvocationException(PartyCodes.E_NO_SUCH_PARTY);
        }
        tuple.right.joinParty(tuple.left, partyId, name, groupRank, rl);
    }

    // from PeerPartyProvider
    public void getPartyScene (
        ClientObject caller, int partyId, InvocationService.ResultListener rl)
        throws InvocationException
    {
        // this will only be run on the appropriate node
        PartyManager mgr = _parties.get(partyId);
        if (mgr == null) {
            log.warning("Party not found on node, should be present", "partyId", partyId);
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
        }
        rl.requestProcessed(mgr.getSceneId());
    }

    // from PartyBoardProvider
    public void createParty (
        ClientObject caller, String name, int groupId, boolean inviteAllFriends,
        InvocationService.ResultListener rl)
        throws InvocationException
    {
        final MemberObject member = (MemberObject)caller;

        if (member.partyId != 0) {
            // TODO: possibly a better error? Surely this will be blocked on the client
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
        }
        // verify that the user is at least a member of the specified group
        GroupMembership groupInfo = member.groups.get(groupId);
        if (groupInfo == null) {
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR); // shouldn't happen
        }
        // TODO: validate with that group who can create parties (may just be managers)
        // TODO: any other party creation restriction checks

        // set up the new PartyObject
        PartyObject pobj = _omgr.registerObject(new PartyObject());
        pobj.id = _peerMgr.getNextPartyId();
        pobj.name = name;
        pobj.group = groupInfo.group;
        pobj.leaderId = member.getMemberId();
        if (member.location instanceof ScenePlace) {
            pobj.sceneId = ((ScenePlace) member.location).sceneId;
        }
        pobj.setAccessController(_partyAccessController);

        // Create the PartyManager and add the member
        PartyManager mgr = _injector.getInstance(PartyManager.class);
        mgr.init(pobj);
        boolean success = false;
        try {
            mgr.addPlayer(member.memberName, groupInfo.rank, rl);
            // if we return from that without throwing an Exception, then we are success
            success = true;

        } finally {
            // do any final poo TODO: this could change as I learn more about the natural lifecycle
            if (success) {
                // register the party
                _parties.put(pobj.id, mgr);
                // set the partyId
                member.setPartyId(pobj.id);

            } else {
                // kill the party object we created
                _omgr.destroyObject(pobj.getOid());
            }
        }
    }

    // from PartyBoardProvider & PeerPartyProvider
    public void getPartyDetail (
        ClientObject caller, int partyId, InvocationService.ResultListener rl)
        throws InvocationException
    {
        // see if we can handle it locally
        PartyManager mgr = _parties.get(partyId);
        if (mgr != null) {
            rl.requestProcessed(mgr.getPartyDetail());
            return;
        }

        Tuple<Client,PeerPartyService> tuple = locatePeerService(partyId);
        if (tuple == null) {
            throw new InvocationException(PartyCodes.E_NO_SUCH_PARTY);
        }
        tuple.right.getPartyDetail(tuple.left, partyId, rl);
    }

    /**
     * Called by a PartyManager when it's removed.
     */
    void partyWasRemoved (int partyId)
    {
        // TODO: this will get more complicated
        _parties.remove(partyId);
    }

    /**
     * Can the specified member access this party?
     */
    protected boolean canAccessParty (PartyInfo party, MemberObject member)
    {
        switch (party.recruitment) {
        case PartyCodes.RECRUITMENT_OPEN:
            return true;

        default:
        case PartyCodes.RECRUITMENT_CLOSED:
            return false;

        case PartyCodes.RECRUITMENT_GROUP:
            return member.isGroupMember(party.group.getGroupId());
        }
    }

    /**
     * Compute the score for the specified party, or return null if the user
     * does not have access to it.
     */
    protected PartySort computePartySort (PartyInfo party, MemberObject member)
    {
        // start by giving you a score of 100 * your rank in the group. (0, 100, or 200)
        int score = 100 * member.getGroupRank(party.group.getGroupId());
        // give additional score if your friend is leading the party
        if (member.isFriend(party.leaderId)) {
            score += 250;
        }
        return new PartySort(score, party.id);
    }

    /**
     * Find and return the PeerPartyService for the specified partyId.
     */
    protected Tuple<Client,PeerPartyService> locatePeerService (int partyId)
    {
        final Integer partyKey = partyId;
        final Object[] result = new Object[1];

        _peerMgr.invokeOnNodes(new Function<Tuple<Client,NodeObject>,Void>() {
            public Void apply (Tuple<Client,NodeObject> clinode) {
                if (result[0] == null) {
                    MsoyNodeObject mnode = (MsoyNodeObject)clinode.right;
                    if (mnode.parties.containsKey(partyKey)) {
                        result[0] = new Tuple<Client,PeerPartyService>(
                            clinode.left, mnode.peerPartyService);
                    }
                }
                return null; // Void
            }
        });

        @SuppressWarnings("unchecked") // fucking A
        Tuple<Client,PeerPartyService> retval = (Tuple<Client,PeerPartyService>)result[0];
        return retval;
    }

    protected AccessController _partyAccessController = new AccessController()
    {
        // documentation inherited from interface
        public boolean allowSubscribe (DObject object, Subscriber<?> sub)
        {
            // if the subscriber is a client, ensure that they are in this party
            if (PresentsSession.class.isInstance(sub)) {
                MemberObject mobj = (MemberObject)PresentsSession.class.cast(sub).getClientObject();
                PartyObject partyObj = (PartyObject)object;
                return mobj.partyId == partyObj.id;
            }

            // else: server
            return true;
        }

        // documentation inherited from interface
        public boolean allowDispatch (DObject object, DEvent event)
        {
            return true; // TODO
        }
    };

    /** Holds compared order between parties without having to recompute it for
     * every comparison. */
    protected static class PartySort
        implements Comparable<PartySort>
    {
        public PartySort (int score, int id)
        {
            _score = score;
            _id = id;
        }

        // NOTE: we do not implement equals or hashCode. All scores are not equal.

        // from Comparable
        public int compareTo (PartySort other)
        {
            // reverse the order, so that higher scores are first
            int cmp = Comparators.compare(other._score, _score);
            if (cmp == 0) {
                // but lower partyIds take priority
                cmp = Comparators.compare(_id, other._id);
            }
            return cmp;
        }

        protected int _score;
        protected int _id;
    } // end: class PartySort

    protected IntMap<PartyManager> _parties = IntMaps.newHashIntMap();

    protected static final int PARTIES_PER_BOARD = 16;

    @Inject protected Injector _injector;
    protected InvocationManager _invmgr;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected MsoyPeerManager _peerMgr;
}
