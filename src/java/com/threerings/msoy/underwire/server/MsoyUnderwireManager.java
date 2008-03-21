//
// $Id$

package com.threerings.msoy.underwire.server;

import com.samskivert.jdbc.depot.PersistenceContext;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.underwire.server.persist.EventRecord;
import com.threerings.underwire.server.persist.UnderwireRepository;

import com.threerings.underwire.web.data.Event;
import com.threerings.msoy.server.persist.OOOUserRecord;
import com.samskivert.io.PersistenceException;

/**
 * Handles generating events for underwire.
 */
public class MsoyUnderwireManager
{
    /**
     * Register out underwire repository.
     */
    public void init (PersistenceContext perCtx)
    {
        _underrepo = new UnderwireRepository(perCtx);
    }

    /**
     * Adds an auto-ban event record to the user.  This is used by the authentication domain and
     * should only be called when on the auth or invoker thread.
     */
    public void reportAutoBan (OOOUserRecord user, String reason)
        throws PersistenceException
    {
        MemberName name = MsoyServer.memberRepo.loadMemberName(user.email);
        EventRecord event = new EventRecord();
        event.source = Integer.toString(name.getMemberId());
        event.sourceHandle = name.toString();
        event.status = Event.RESOLVED_CLOSED;
        event.subject = reason;
        _underrepo.insertEvent(event);
    }

    protected UnderwireRepository _underrepo;
}
