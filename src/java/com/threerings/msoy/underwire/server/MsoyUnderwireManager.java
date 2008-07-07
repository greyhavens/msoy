//
// $Id$

package com.threerings.msoy.underwire.server;

import java.lang.StringBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.user.OOOUser;

import com.threerings.underwire.server.persist.EventRecord;
import com.threerings.underwire.server.persist.UnderwireRepository;
import com.threerings.underwire.web.data.Event;

import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.data.ChannelMessage;
import com.threerings.msoy.chat.data.ChatChannel;

import static com.threerings.msoy.Log.log;

/**
 * Handles generating events for underwire.
 */
@Singleton
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
     * Adds an auto-ban event record to the user.
     */
    @BlockingThread
    public void reportAutoBan (OOOUser user, String reason)
        throws PersistenceException
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
    @EventThread
    public void addComplaint (final MemberObject source, final int targetId, String complaint)
    {
        final EventRecord event = new EventRecord();
        event.source = Integer.toString(source.memberName.getMemberId());
        event.sourceHandle = source.memberName.toString();
        event.status = Event.OPEN;
        event.subject = complaint;

        // format and provide the complainer's chat history
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        StringBuilder chatHistory = new StringBuilder();
        for (ChatMessage msg : SpeakUtil.getChatHistory(source.memberName)) {
            UserMessage umsg = (UserMessage)msg;
            chatHistory.append(df.format(new Date(umsg.timestamp))).append(' ');
            if (umsg instanceof ChannelMessage) {
                ChannelMessage cmsg = (ChannelMessage)umsg;
                chatHistory.append('[').append(ChatChannel.XLATE_TYPE[cmsg.channel.type]);
                chatHistory.append(':').append(cmsg.channel.ident).append("] ");
            } else {
                chatHistory.append(StringUtil.pad(ChatCodes.XLATE_MODES[umsg.mode], 10)).append(' ');
            }
            chatHistory.append(umsg.speaker);
            if (umsg.speaker instanceof MemberName) {
                chatHistory.append('(').append(((MemberName)umsg.speaker).getMemberId()).append(')');
            }
            chatHistory.append(": ").append(umsg.message).append('\n');
        }
        event.chatHistory = chatHistory.toString();

        // if the target is online, get thir name from their member object
        MemberObject target = _locator.lookupMember(targetId);
        if (target != null) {
            event.targetHandle = target.memberName.toString();
            event.target = Integer.toString(target.memberName.getMemberId());
        }

        _invoker.postUnit(new Invoker.Unit("addComplaint") {
            public boolean invoke () {
                try {
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
                } catch (PersistenceException pe) {
                    log.warning("Failed to add complaint event [event=" + event + "].");
                    _failed = true;
                }
                return true;
            }
            public void handleResult () {
                SpeakUtil.sendFeedback(source, MsoyCodes.GENERAL_MSGS,
                        _failed ? "m.complain_fail" : "m.complain_success");
            }
            protected boolean _failed = false;
        });
    }

    /**
     * Adds a mesasge complaint to the even queue.
     */
    @BlockingThread
    public void addMessageComplaint (MemberName source, int targetId, String message,
                                     String subject, String link)
        throws PersistenceException
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
