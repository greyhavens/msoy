//
// $Id$

package com.threerings.msoy.chat.server;

import java.util.HashMap;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.crowd.chat.server.SpeakDispatcher;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.chat.client.ChatChannelService;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelCodes;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.ChatterInfo;

import static com.threerings.msoy.Log.log;

/**
 * Manages the server side of our chat channel services.
 */
public class ChatChannelManager
    implements ChatChannelProvider, ChatChannelCodes
{
    /**
     * Initializes this manager during server startup.
     */
    public void init (InvocationManager invmgr)
    {
        // register our chat channel service
        invmgr.registerDispatcher(new ChatChannelDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

    // from interface ChatChannelProvider
    public void joinChannel (ClientObject caller, ChatChannel channel,
                             ChatChannelService.ResultListener listener)
        throws InvocationException
    {
        MemberObject user = (MemberObject)caller;

        // ensure this member has access to this channel
        switch (channel.type) {
        case ChatChannel.GROUP_CHANNEL:
            if (!user.groups.containsKey((GroupName)channel.ident)) {
                log.info("Member requested to join group channel of which they are not a member " +
                         "[who=" + user.who() + ", channel=" + channel + "].");
                throw new InvocationException(E_ACCESS_DENIED);
            }
            break;

        case ChatChannel.PRIVATE_CHANNEL:
            // TODO
            break;

        default:
            log.warning("Member requested to join invalid channel [who=" + user.who() +
                        ", channel=" + channel + "].");
            throw new InvocationException(E_INTERNAL_ERROR);
        }

        // make sure the channel is still around
        ChatChannelObject ccobj = _channels.get(channel);
        if (ccobj == null) {
            // if this is a group channel, we create them on demand
            if (channel.type == ChatChannel.GROUP_CHANNEL) {
                ccobj = createChannel(channel);
            } else {
                // otherwise the channel is gone baby gone
                throw new InvocationException(E_NO_SUCH_CHANNEL);
            }
        }

        // add the caller to the channel (if they're not already for some reason)
        if (!ccobj.chatters.containsKey(user.memberName)) {
            log.info("Adding " + user.who() + " to " + channel + ".");
            ccobj.addToChatters(new ChatterInfo(user));
        }

        // and let them know that they're good to go
        listener.requestProcessed(ccobj.getOid());
    }

    // from interface ChatChannelProvider
    public void leaveChannel (ClientObject caller, ChatChannel channel)
    {
        MemberObject user = (MemberObject)caller;
        ChatChannelObject ccobj = _channels.get(channel);
        if (ccobj != null && ccobj.chatters.containsKey(user.memberName)) {
            log.info("Removing " + user.who() + " from " + ccobj.channel + ".");
            ccobj.removeFromChatters(user.memberName);
        }
    }

    // from interface ChatChannelProvider
    public void inviteToChannel (ClientObject caller, MemberName target, ChatChannel channel,
                                 ChatChannelService.ConfirmListener listener)
        throws InvocationException
    {
        // TODO
    }

    // from interface ChatChannelProvider
    public void createChannel (ClientObject caller, String name,
                               ChatChannelService.ResultListener listener)
        throws InvocationException
    {
        MemberObject user = (MemberObject)caller;
        ChatChannel channel = ChatChannel.makePrivateChannel(
            new ChannelName(name, user.getMemberId()));

        // if they've already got a channel with this name (maybe they created a chat channel,
        // invited people to it, then left and are now "recreating it"), just give them back the
        // original channel; otherwise create it 
        ChatChannelObject ccobj = _channels.get(channel);
        if (ccobj == null) {
            ccobj = createChannel(channel);
        }

        // TODO: add a listener to the channel object and shut it down when the last chatter leaves

        log.info("Adding " + user.who() + " to " + channel + ".");
        ccobj.addToChatters(new ChatterInfo(user));
        listener.requestProcessed(ccobj.getOid());
    }

    /**
     * Creates a new chat channel object, registers it in the necessary places and returns it.
     */
    protected ChatChannelObject createChannel (ChatChannel channel)
    {
        assert(!_channels.containsKey(channel));
        log.info("Creating chat channel " + channel + ".");

        final ChatChannelObject ccobj = MsoyServer.omgr.registerObject(new ChatChannelObject());
        ccobj.channel = channel;
        SpeakProvider.SpeakerValidator validator = new SpeakProvider.SpeakerValidator() {
            public boolean isValidSpeaker (DObject speakObj, ClientObject speaker, byte mode) {
                MemberObject who = (MemberObject)speaker;
                return (who == null || ccobj.chatters.containsKey(who.memberName));
            }
        };
        ccobj.setSpeakService((SpeakMarshaller)MsoyServer.invmgr.registerDispatcher(
                                  new SpeakDispatcher(new SpeakProvider(ccobj, validator))));
        _channels.put(channel, ccobj);
        return ccobj;
    }

    /** Contains a mapping of all active chat channels. */
    protected HashMap<ChatChannel,ChatChannelObject> _channels =
        new HashMap<ChatChannel,ChatChannelObject>();
}
