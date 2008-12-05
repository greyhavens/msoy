//
// $Id$

package com.threerings.msoy.party.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.group.data.all.GroupMembership;

import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyObject;

@Singleton
public class PartyRegistry
    implements PartyBoardProvider
{
    @Inject public PartyRegistry (InvocationManager invmgr)
    {
        invmgr.registerDispatcher(new PartyBoardDispatcher(this), MsoyCodes.MEMBER_GROUP);
    }

    /**
     * Called to initialize the PartyRegistry after server startup.
     */
    public void init ()
    {
        // nada, presently
    }

    // from PartyBoardProvider
    public void getPartyBoard (
        ClientObject caller, String query, InvocationService.ResultListener rl)
        throws InvocationException
    {
        MemberObject member = (MemberObject)caller;

        System.err.println("Asked for the party board: " + member.who());

        // TODO!
        throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
    }

    // from PartyBoardProvider
    public void joinParty (ClientObject caller, int partyId, InvocationService.ResultListener rl)
        throws InvocationException
    {
        MemberObject member = (MemberObject)caller;

        // right now, we just pass the buck to the PartyManager
        PartyManager mgr = _parties.get(partyId);
        if (mgr == null) {
            throw new InvocationException(PartyCodes.E_NO_SUCH_PARTY);
        }

        // pass the buck completely to the manager
        mgr.addMate(member, rl);
    }

    // from PartyProvider
    public void createParty (
        ClientObject caller, String name, int groupId, InvocationService.ResultListener rl)
        throws InvocationException
    {
         MemberObject member = (MemberObject)caller;

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
        pobj.id = _nextPartyId++;
        pobj.name = name;
        pobj.group = groupInfo.group;
        pobj.leaderId = member.getMemberId();
        pobj.setAccessController(_partyAccessController);

        // Create the PartyManager and add the member
        PartyManager mgr = new PartyManager(pobj);
        boolean success = false;
        try {
            mgr.addMate(member, rl);
            // if we return from that without throwing an Exception, then we are success
            success = true;

        } finally {
            // do any final poo TODO: this could change as I learn more about the natural lifecycle
            if (success) {
                // register the party
                _parties.put(pobj.id, mgr);

            } else {
                // kill the party object we created
                _omgr.destroyObject(pobj.getOid());
            }
        }
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

    protected IntMap<PartyManager> _parties = IntMaps.newHashIntMap();

    /** Assigns the next party id. */
    protected int _nextPartyId = 1; // TODO: per-node magicy, like guestIds.

    @Inject protected RootDObjectManager _omgr;
}
