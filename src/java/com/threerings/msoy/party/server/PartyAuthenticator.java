//
// $Id$

package com.threerings.msoy.party.server;

import java.util.concurrent.Callable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.util.ServiceWaiter;

import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.msoy.data.AuthName;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.AuxAuthenticator;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.party.data.PartyAuthName;
import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyCredentials;

import static com.threerings.msoy.Log.log;

/**
 * Handles partier authentication.
 */
@Singleton
public class PartyAuthenticator extends AuxAuthenticator<PartyCredentials>
{
    protected PartyAuthenticator ()
    {
        super(PartyCredentials.class);
    }

    @Override // from AuxAuthenticator
    protected AuthName createName (String accountName, int memberId)
    {
        return new PartyAuthName(accountName, memberId);
    }

    @Override // from AuxAuthenticator
    protected void finishAuthentication (PartyCredentials creds, final MemberName name)
        throws ServiceException
    {
        final int partyId = creds.partyId;

        // we need to find out the group id of the party in question
        int groupId = eventCall(new Callable<Integer>() {
            public Integer call () throws Exception {
                return _partyReg.getPartyGroupId(partyId);
            }
        });
        if (groupId == 0) {
            throw new ServiceException(PartyCodes.E_NO_SUCH_PARTY);
        }

        // load up our rank in the group in question
        final byte groupRank = _groupRepo.getRank(groupId, name.getMemberId());

        // now we can pre-join the party (to reserve our spot and make sure we're allowed in)
        eventCall(new Callable<Void>() {
            public Void call () throws Exception {
                _partyReg.preJoinParty(name, partyId, groupRank);
                return null;
            }
        });
    }

    /**
     * Executes some code on the dobj event thread, waits for the result and returns it. Any
     * failure is rewrapped in a ServiceException.
     */
    protected <T> T eventCall (final Callable<T> callable)
        throws ServiceException
    {
        final ServiceWaiter<T> waiter = new ServiceWaiter<T>(10);
        _omgr.postRunnable(new Runnable () {
            public void run () {
                try {
                    waiter.postSuccess(callable.call());
                } catch (Exception e) {
                    waiter.postFailure(e);
                }
            }
        });

        try {
            if (!waiter.waitForResponse()) {
                throw new ServiceException(waiter.getError().getMessage());
            }
            return waiter.getArgument();
        } catch (ServiceWaiter.TimeoutException te) {
            log.warning("Party authentication timed out!", te);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }
    }

    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected PartyRegistry _partyReg;
    @Inject protected GroupRepository _groupRepo;
}
