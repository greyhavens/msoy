//
// $Id$

package com.threerings.msoy.chat.server;

import java.util.HashMap;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.crowd.chat.server.SpeakDispatcher;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupMembership;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.web.data.Group;

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
    public void joinChannel (ClientObject caller, final ChatChannel channel,
                             final ChatChannelService.ResultListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject)caller;

        // ensure this member has access to this channel
        switch (channel.type) {
        case ChatChannel.GROUP_CHANNEL:
            GroupMembership gm = user.groups.get((GroupName) channel.ident);
            if (gm != null) {
                // we're already members, no need to check further
                break;
            }

            // else check that the group is public
            final GroupName gName = (GroupName) channel.ident;
            MsoyServer.invoker.postUnit(new RepositoryUnit("joinChannel") {
                public void invokePersist () throws PersistenceException {
                    GroupRecord gRec = MsoyServer.groupRepo.loadGroup(gName.getGroupId());
                    _policy = (gRec == null) ? 0 : gRec.policy;
                }
                public void handleSuccess () {
                    if (_policy == Group.POLICY_PUBLIC) {
                        finishJoining(user, channel, listener);
                    } else {
                        log.warning("Unable to join non-public channel [user=" + user +
                                    ", channel=" + channel + "]");
                        listener.requestFailed(E_ACCESS_DENIED);
                    }
                }
                public void handleFailure (Exception pe) {
                    log.warning("Unable to load group [group=" + gName +
                                ", error=" + pe + ", cause=" + pe.getCause() + "]");
                    listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
                }
                protected int _policy;
            });
            return;

        case ChatChannel.PRIVATE_CHANNEL:
            // TODO: access controls
            break;

        default:
            log.warning("Member requested to join invalid channel [who=" + user.who() +
                        ", channel=" + channel + "].");
            throw new InvocationException(E_INTERNAL_ERROR);
        }

        // if we made it this far, we can do our joining immediately
        finishJoining(user, channel, listener);
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

        log.info("Adding " + user.who() + " to " + channel + ".");
        ccobj.addToChatters(new ChatterInfo(user));
        listener.requestProcessed(ccobj.getOid());
    }

    /**
     * Enumerate all the existing active chat channels.
     */
    public Iterable<ChatChannel> getChatChannels ()
    {
        return _channels.keySet();
    }

    /**
     * Creates a new chat channel object, registers it in the necessary places and returns it.
     */
    protected ChatChannelObject createChannel (ChatChannel channel)
    {
        assert(!_channels.containsKey(channel));
        log.info("Creating chat channel " + channel + ".");

        // create and initialize the chat channel object
        final ChatChannelObject ccobj = MsoyServer.omgr.registerObject(new ChatChannelObject());
        ccobj.channel = channel;
        SpeakProvider.SpeakerValidator validator = new SpeakProvider.SpeakerValidator() {
            public boolean isValidSpeaker (DObject speakObj, ClientObject speaker, byte mode) {
                MemberObject who = (MemberObject)speaker;
                return (who == null || ccobj.chatters.containsKey(who.memberName));
            }
        };
        SpeakDispatcher sd = new SpeakDispatcher(new SpeakProvider(ccobj, validator));
        ccobj.setSpeakService((SpeakMarshaller)MsoyServer.invmgr.registerDispatcher(sd));
        // add a listener to the channel object to shut it down when the last chatter leaves
        ccobj.addListener(new ShutdownListener(ccobj));
        // map the channel to its distributed object
        _channels.put(channel, ccobj);

        return ccobj;
    }

    protected void finishJoining (MemberObject user, ChatChannel channel,
        ChatChannelService.ResultListener listener)
    {
        // make sure the channel is still around
        ChatChannelObject ccobj = _channels.get(channel);
        if (ccobj == null) {
            // if this is a group channel, we create them on demand
            if (channel.type == ChatChannel.GROUP_CHANNEL) {
                ccobj = createChannel(channel);
            } else {
                // otherwise the channel is gone baby gone
                listener.requestFailed(E_NO_SUCH_CHANNEL);
                return;
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

    /**
     * Called when the last chatter leaves a channel, cleans up and destroys the channel.
     */
    protected void shutdownChannel (ChatChannel channel)
    {
        ChatChannelObject ccobj = _channels.remove(channel);
        if (ccobj == null) {
            log.warning("Requested to shutdown unmapped channel [channel=" + channel + "].");
        } else {
            log.info("Shutting down chat channel: " + channel + ".");
            MsoyServer.invmgr.clearDispatcher(ccobj.speakService);
            MsoyServer.omgr.destroyObject(ccobj.getOid());
        }
    }

    protected class ShutdownListener extends SetAdapter
    {
        public ShutdownListener (ChatChannelObject chobj) {
            _chobj = chobj;
        }
        public void entryRemoved (EntryRemovedEvent event) {
            if (ChatChannelObject.CHATTERS.equals(event.getName())) {
                if (_chobj.chatters.size() == 0) {
                    // this will destroy the object so we need not stop listening
                    shutdownChannel(_chobj.channel);
                }
            }
        }
        protected ChatChannelObject _chobj;
    };

    /** Contains a mapping of all active chat channels. */
    protected HashMap<ChatChannel,ChatChannelObject> _channels =
        new HashMap<ChatChannel,ChatChannelObject>();
}
