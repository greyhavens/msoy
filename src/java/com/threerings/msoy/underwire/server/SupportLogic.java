//
// $Id$

package com.threerings.msoy.underwire.server;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.DObject;

import com.threerings.user.OOOUser;

import com.threerings.underwire.server.persist.EventRecord;
import com.threerings.underwire.server.persist.UnderwireRepository;
import com.threerings.underwire.web.data.Event;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.server.SpeakUtil;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.data.MsoyCodes;
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
     * Compile server-side information for a complaint against a MemberObject or a PlayerObject
     * and file it with the Underwire system. Note that this method must be called on the dobj
     * thread. The target name is optional (only available when the complainee is online).
     */
    @EventThread
    public void complainMember (
        final DObject complainerObj, MemberName complainerName, final int targetId,
        String complaint, MemberName optTargetName)
    {
        final EventRecord event = new EventRecord();
        event.source = Integer.toString(complainerName.getMemberId());
        event.sourceHandle = complainerName.toString();
        event.status = Event.OPEN;
        event.subject = complaint;

        // format and provide the complainer's chat history
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        StringBuilder chatHistory = new StringBuilder();
        for (ChatMessage msg : SpeakUtil.getChatHistory(complainerName)) {
            UserMessage umsg = (UserMessage)msg;
            chatHistory.append(df.format(new Date(umsg.timestamp))).append(' ').
                append(StringUtil.pad(ChatCodes.XLATE_MODES[umsg.mode], 10)).append(' ').
                append(umsg.speaker);
            if (umsg.speaker instanceof MemberName) {
                chatHistory.append('(').append(((MemberName)umsg.speaker).getMemberId()).append(')');
            }
            chatHistory.append(": ").append(umsg.message).append('\n');
        }
        event.chatHistory = chatHistory.toString();

        if (optTargetName != null) {
            event.targetHandle = optTargetName.toString();
            event.target = Integer.toString(optTargetName.getMemberId());
        }

        _invoker.postUnit(new Invoker.Unit("addComplaint") {
            @Override public boolean invoke () {
                try {
                    addComplaint(event, targetId);
                } catch (Exception e) {
                    log.warning("Failed to add complaint event [event=" + event + "].", e);
                    _failed = true;
                }
                return true;
            }
            @Override public void handleResult () {
                SpeakUtil.sendFeedback(complainerObj, MsoyCodes.GENERAL_MSGS,
                        _failed ? "m.complain_fail" : "m.complain_success");
            }
            protected boolean _failed = false;
        });
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
                event.targetHandle = "Guest";
            } else {
                event.targetHandle = targetName.toString();
                event.target = Integer.toString(targetName.getMemberId());
            }
        }

        // add the event to the repository
        _underrepo.insertEvent(event);
    }

    /**
     * Adds a message complaint to the event queue.
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
