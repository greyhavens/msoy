//
// $Id$

package com.threerings.msoy.party.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.HashIntMap;

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

import com.threerings.msoy.party.data.PartyObject;

// import com.threerings.msoy.data.MemberObject;

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

        // TODO!
        throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);

//        PartyObject pobj = _parties.get(partyId);
//
//        // TODO: Don't create a party if the partyId isn't found
//        if (pobj == null) {
//            startParty(caller, "hi", rl);
//            pobj = _parties.get(partyId);
//        }
//
//        // TODO: Validate permission to join party
//
////        member.setPartyId(partyId);
    }

    // from PartyProvider
    public void createParty (
        ClientObject caller, String name, int groupId, InvocationService.ResultListener rl)
        throws InvocationException
    {
        MemberObject member = (MemberObject)caller;

        // TODO!
        throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);

//        // TODO!
//        throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
//
//        int partyId = 1; // TODO
//
//        PartyObject pobj = _omgr.registerObject(new PartyObject());
////        pobj.name = name;
//        pobj.id = partyId;
//
//        pobj.setAccessController(_partyAccessController);
//
//        _parties.put(partyId, pobj);

        // TODO: Call joinParty here to add the caller to the new party
        // After joinParty doesn't call on startParty
    }

    protected HashIntMap<PartyManager> _parties = new HashIntMap<PartyManager>();

    protected AccessController _partyAccessController = new AccessController()
    {
        // documentation inherited from interface
        public boolean allowSubscribe (DObject object, Subscriber<?> sub)
        {
            // if the subscriber is a client, ensure that they are this same user
            if (PresentsSession.class.isInstance(sub)) {
//                 MemberObject mobj = (MemberObject)PresentsSession.class.cast(sub).getClientObject();
//                 PartyObject pobj = (PartyObject)object;
//                 return mobj.partyId == pobj.partyId;
                return true;
            }
            return true;
        }
        // documentation inherited from interface
        public boolean allowDispatch (DObject object, DEvent event)
        {
            return true; // TODO
        }
    };

    @Inject protected RootDObjectManager _omgr;
}
