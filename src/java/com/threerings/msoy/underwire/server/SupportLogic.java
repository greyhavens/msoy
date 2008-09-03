//
// $Id$

package com.threerings.msoy.underwire.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.MainInvoker;

import com.threerings.user.OOOUser;

import com.threerings.underwire.server.persist.EventRecord;
import com.threerings.underwire.server.persist.UnderwireRepository;
import com.threerings.underwire.web.data.Event;

import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.data.all.MemberName;

import static com.threerings.msoy.Log.log;

/**
 * Handles generating events for the Underwire support system.
 */
@Singleton @BlockingThread
public class SupportLogic
{
    @Inject public SupportLogic (PersistenceContext perCtx)
    {
        _underrepo = new UnderwireRepository(perCtx);
    }

    /**
     * Adds an auto-ban event record to the user.
     */
    public void reportAutoBan (OOOUser user, String reason)
    {
        MemberName name = _memberRepo.loadMemberName(user.email);
        EventRecord event = new EventRecord();
        event.source = Integer.toString(name.getMemberId());
        event.sourceHandle = name.toString();
        event.status = Event.RESOLVED_CLOSED;
        event.subject = reason;
        _underrepo.insertEvent(event);
    }

    /**
     * Adds a complaint record to the underwire event queue.
     */
    public void addComplaint (EventRecord event, int targetId)
    {
        // load the target information if necessary
        if (event.target == null) {
            MemberName targetName = _memberRepo.loadMemberName(targetId);
            if (targetName == null) {
                log.warning("Unable to locate target of complaint [event=" + event +
                            ", targetId=" + targetId + "].");
            } else {
                event.targetHandle = targetName.toString();
                event.target = Integer.toString(targetName.getMemberId());
            }
        }

        // add the event to the repository
        _underrepo.insertEvent(event);
    }

    /**
     * Adds a mesasge complaint to the even queue.
     */
    public void addMessageComplaint (MemberName source, int targetId, String message,
                                     String subject, String link)
    {
        final EventRecord event = new EventRecord();
        event.source = Integer.toString(source.getMemberId());
        event.sourceHandle = source.toString();
        event.status = Event.OPEN;
        event.subject = subject;
        event.link = link;
        event.chatHistory = message.replaceAll("<br/>", "");
        event.target = Integer.toString(targetId);
        MemberName target = _memberRepo.loadMemberName(targetId);
        if (target == null) {
            log.warning("Unable to locate target of complaint [event=" + event +
                    ", targetId=" + targetId+ "].");
        } else {
            event.targetHandle = target.toString();
        }
        _underrepo.insertEvent(event);
    }

    protected UnderwireRepository _underrepo;

    @Inject protected MemberLocator _locator;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected @MainInvoker Invoker _invoker;
}
