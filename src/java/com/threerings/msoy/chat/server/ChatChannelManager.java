//
// $Id$

package com.threerings.msoy.chat.server;

import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ResultListener;
import com.samskivert.jdbc.RepositoryUnit;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;
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
import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.data.PeerChatMarshaller;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.peer.server.PeerChatDispatcher;
import com.threerings.msoy.peer.server.PeerChatProvider;
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
    implements ChatChannelProvider, ChatChannelCodes, PeerChatProvider
{
    /**
     * Initializes this manager during server startup.
     */
    public void init (InvocationManager invmgr)
    {
        // register our chat channel service
        invmgr.registerDispatcher(new ChatChannelDispatcher(this), MsoyCodes.WORLD_GROUP);

        // register and initialize our peer chat service
        InvocationMarshaller marshaller = invmgr.registerDispatcher(new PeerChatDispatcher(this));
        MsoyNodeObject me = (MsoyNodeObject) MsoyServer.peerMan.getNodeObject();
        me.setPeerChatService((PeerChatMarshaller)marshaller);
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
                        resolveAndJoinChannel(user, channel, listener);
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
        resolveAndJoinChannel(user, channel, listener);
    }
    
    // from interface ChatChannelProvider
    public void leaveChannel (ClientObject caller, ChatChannel channel)
    {
        MemberObject user = (MemberObject)caller;
        ChatChannelObject ccobj = _knownChannels.get(channel);
        if (ccobj != null && ccobj.chatters.containsKey(user.memberName)) {
            log.info("Removing " + user.who() + " from " + ccobj.channel + ".");
            removeChatter(ccobj, user); // this will also clean up the channel if needed
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
                               final ChatChannelService.ResultListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject)caller;
        final ChatChannel channel = ChatChannel.makePrivateChannel(
            new ChannelName(name, user.getMemberId()));

        resolveAndJoinChannel(user, channel, listener);
    }

    // from interface PeerChatProvider
    public void addUser (ClientObject caller, ChatterInfo chatter, ChatChannel channel,
                         PeerChatService.ConfirmListener listener)
        throws InvocationException
    {
        ChatChannelObject ccobj = _knownChannels.get(channel);
        if (ccobj == null) {
            listener.requestFailed("The host does not recognize this channel - dobj not found.");
            return;
        }
        
        ccobj.addToChatters(chatter);

        listener.requestProcessed();
    }
    
    // from interface PeerChatProvider
    public void removeUser (ClientObject caller, ChatterInfo chatter, ChatChannel channel,
                            PeerChatService.ConfirmListener listener)
        throws InvocationException
    {
        ChatChannelObject ccobj = _knownChannels.get(channel);
        if (ccobj == null) {
            listener.requestFailed("The host does not recognize this channel - dobj not found.");
            return;
        }
        
        ccobj.removeFromChatters(chatter.getKey());

        listener.requestProcessed();
    }
        
    /**
     * Enumerate all the existing chat channels, both subscribed and hosted.
     */
    public Iterable<ChatChannel> getChatChannels ()
    {
        return _knownChannels.keySet();
    }

    /**
     * Creates or subscribes to the specified channel, and adds the player.
     */
    protected void resolveAndJoinChannel (final MemberObject user, final ChatChannel channel,
                                          final ChatChannelService.ResultListener listener)
    {
        withResolvedChannel(channel, new ChannelFinalizer(user, channel, listener) {
            public void requestCompleted (ChatChannelObject ccobj,
                                          final ChatChannelService.ResultListener listener) {
                // now that we have the channel, try to add the user
                addChatter(ccobj, user);
                // and let the caller know that they're good to go
                listener.requestProcessed(ccobj.getOid());
            }
        });
    }
    
    /**
     * Ensures that the channel exists and is available (either by subscribing to it,
     * or by creating it from scratch). When the channel is ready, calls the listener.
     */
    protected void withResolvedChannel (final ChatChannel channel, final ChannelFinalizer cont)
    {
        // check if we already have it
        ChatChannelObject ccobj = _knownChannels.get(channel);
        if (ccobj != null) {
            // we already have this channel, either because we're hosting it, or because
            // we're subscribed to the real host. let the caller know.
            cont.requestCompleted(ccobj);
            return;
        }

        // we don't have the channel. is it already hosted on another server?
        MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(channel);
        if (host != null) {
            log.info("Subscribing to another host peer: " + host.nodeName);
            createSubscriptionChannel(channel, host, cont);
            return;
        }

        // nobody has this channel. let's create and host one.
        NodeObject.Lock lock = MsoyPeerManager.getChannelLock(channel);
        PeerManager.LockedOperation createOp = new PeerManager.LockedOperation() {
            public void run () {
                log.info("Got lock, creating channel " + channel);
                createHostedChannel(channel, cont);
            }
            public void fail (String peerName) {
                log.info("Another peer got the lock: " + peerName);
                if (peerName != null) {
                    cont.requestFailed(ChatChannelCodes.E_INTERNAL_ERROR);
                    // todo: we should try subscribing to the peer's new channel, not just give up
                }
            }
        };
        MsoyServer.peerMan.performWithLock(lock, createOp);
    }

    /**
     * Registers to a channel hosted on another peer.
     */
    protected void createSubscriptionChannel (final ChatChannel channel, MsoyNodeObject host,
                                              final ChannelFinalizer cont)
    {
        // after we've subscribed, here's what we'll do: create a new local "channel",
        // and initialize the proxied channel distributed object.
        ResultListener<Integer> subscriptionResult = new ResultListener<Integer>() {
            public void requestCompleted (Integer localOid) {
                // subscription successful! we have the local oid of the proxy object
                ChatChannelObject ccobj = (ChatChannelObject) MsoyServer.omgr.getObject(localOid);
                initializeLocalChannel(ccobj, channel);
                ccobj.addListener(new SubscriptionChannelListener(ccobj));
                cont.requestCompleted(ccobj);
            }
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Channel subscription failed [cause=" + cause + "].");
                cont.requestFailed(cause);
            }
        };

        // now let's try to subscribe
        HostedChannel hosted = host.hostedChannels.get(HostedChannel.getKey(channel));
        if (hosted == null) {
            // the host used to have this channel, but it disappeared. where did it go?
            log.warning("Remote channel no longer hosted, cannot be subscribed! " +
                        "[previous host=" + host + ", channel=" + channel + "].");
            cont.requestFailed(ChatChannelCodes.E_INTERNAL_ERROR);
            return;
        }

        // let's get the proxy object - the listener will do the rest
        MsoyServer.peerMan.proxyRemoteObject(host.nodeName, hosted.oid, subscriptionResult);
    }

    /**
     * Creates a new chat channel object, registers it in the necessary places and returns it.
     */
    protected void createHostedChannel (ChatChannel channel, ChannelFinalizer cont)
    {
        assert(!_knownChannels.containsKey(channel));
        log.info("Creating chat channel " + channel + ".");

        // create and initialize a new chat channel object
        final ChatChannelObject ccobj = MsoyServer.omgr.registerObject(new ChatChannelObject());

        // add a listener to the channel object to destroy it when the last client leaves
        ccobj.addListener(new HostedChannelListener(ccobj));

        // and advertise to other peers that we're hosting this channel
        HostedChannel hosted = new HostedChannel(channel, ccobj.getOid());
        ((MsoyNodeObject) MsoyServer.peerMan.getNodeObject()).addToHostedChannels(hosted);

        initializeLocalChannel(ccobj, channel);
        cont.requestCompleted(ccobj);
    }

    /**
     * Called when the last local client leaves this subscription channel, cleans up
     * and unregisters from the host peer.
     */
    protected void shutdownSubscriptionChannel (final ChatChannelObject ccobj)
    {
        // we need the hosting peer's object Id for this channel - so let's fetch it
        MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(ccobj.channel);
        HostedChannel hostedInfo = host.hostedChannels.get(HostedChannel.getKey(ccobj.channel));
        if (hostedInfo == null) {
            // it went away! 
            log.warning("Remote channel no longer hosted, cannot be unsubscribed! " +
                        "[previous host=" + host + ", channel=" + ccobj.channel + "].");
            return;
        }

        deinitializeLocalChannel(ccobj);
        MsoyServer.peerMan.unproxyRemoteObject(host.nodeName, hostedInfo.oid);
    }

    /**
     * Called when the last chatter leaves a channel, cleans up and destroys the channel.
     */
    protected void shutdownHostedChannel (ChatChannelObject ccobj)
    {
        log.info("Shutting down hosted chat channel: " + ccobj.channel + ".");
        MsoyNodeObject host = (MsoyNodeObject) MsoyServer.peerMan.getNodeObject();
        host.removeFromHostedChannels(HostedChannel.getKey(ccobj.channel));
        deinitializeLocalChannel(ccobj);
        MsoyServer.omgr.destroyObject(ccobj.getOid());
    }

    /**
     * Tells the new channel dobject about the channel, and sets up appropriate listeners.
     */
    protected void initializeLocalChannel (final ChatChannelObject ccobj, ChatChannel channel) 
    {
        ccobj.channel = channel;
        SpeakProvider.SpeakerValidator validator = new SpeakProvider.SpeakerValidator() {
            public boolean isValidSpeaker (DObject speakObj, ClientObject speaker, byte mode) {
                MemberObject who = (MemberObject)speaker;
                return (who == null || ccobj.chatters.containsKey(who.memberName));
            }
        };
        SpeakDispatcher sd = new SpeakDispatcher(new SpeakProvider(ccobj, validator));
        ccobj.setSpeakService((SpeakMarshaller)MsoyServer.invmgr.registerDispatcher(sd));

        // map the channel to its distributed object
        _knownChannels.put(channel, ccobj);
    }

    /** Removes the channel from the list, and severs links from the dobject. */
    protected void deinitializeLocalChannel (final ChatChannelObject ccobj)
    {
        if (ccobj == null) {
            log.warning("Removing null channel object!"); // something went horribly wrong
            return;
        }
        
        MsoyServer.invmgr.clearDispatcher(ccobj.speakService);

        ChatChannelObject removed = _knownChannels.remove(ccobj.channel);
        if (removed != ccobj) {
            log.warning("Removed an unexpected channel [expected=" + ccobj.channel +
                        ", removed=" + removed.channel + "].");
        } 
    }

    /** Am I this channel's host? */
    protected boolean iAmHosting (ChatChannel channel)
    {
        MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(channel);
        MsoyNodeObject me = (MsoyNodeObject) MsoyServer.peerMan.getNodeObject();
        return (host == me);
    }
    
    /**
     * Adds a new participants to a resolved channel object.
     */
    protected void addChatter (ChatChannelObject ccobj, MemberObject user)
    {
        final ChatterInfo userInfo = new ChatterInfo(user);
            
        if (ccobj.chatters.containsKey(user.memberName)) {
            log.warning("User already in chat channel, cannot add [user=" + user.who() +
                        ", channel=" + ccobj.channel + "].");
            return;
        }
        
        if (iAmHosting(ccobj.channel)) {
            try {
                addUser(null, userInfo, ccobj.channel,
                        new ChannelModificationListener(userInfo, ccobj.channel, 0));
            } catch (Exception ex) {
                log.warning("Host failed to add a new user [user=" + user.who() +
                            ", channel=" + ccobj.channel + ", error=" + ex.getMessage() + "].");
            }
        } else {
            // i'm a subscriber. let's ask the host to add the user
            MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(ccobj.channel);
            host.peerChatService.addUser(
                MsoyServer.peerMan.getPeerClient(host.nodeName), userInfo, ccobj.channel,
                new ChannelModificationListener(userInfo, ccobj.channel, 1));
        }
    }
        
    /**
     * Removes participants from a channel. Removal may trigger channel cleanup.
     */
    protected void removeChatter (ChatChannelObject ccobj, MemberObject user)
    {
        final ChatterInfo userInfo = new ChatterInfo(user);
        
        if (! ccobj.chatters.containsKey(user.memberName)) {
            log.warning("User not in chat channel, cannot remove [user=" + user.who() +
                        ", channel=" + ccobj.channel + "].");
            return;
        }
        
        if (iAmHosting(ccobj.channel)) {
            try {
                removeUser(null, userInfo, ccobj.channel,
                           new ChannelModificationListener(userInfo, ccobj.channel, 0));
            } catch (Exception ex) {
                log.warning("Host failed to remove a user [user=" + user.who() +
                            ", channel=" + ccobj.channel + ", error=" + ex.getMessage() + "].");
            }    
        } else {
            // i'm a subscriber. let's ask the host to remove the user
            MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(ccobj.channel);
            host.peerChatService.removeUser(
                MsoyServer.peerMan.getPeerClient(host.nodeName), userInfo, ccobj.channel,
                new ChannelModificationListener(userInfo, ccobj.channel, -1));
        }
    }
    
    /**
     * Returns the number of local clients currently chatting on this channel.
     */
    protected int getLocalChatterCount (ChatChannel channel)
    {
        Integer count = _subscriberCounts.get(channel);
        return (count != null) ? count : 0;
    }

    /**
     * Increments or decrements local chatter count by the specified delta.
     */
    protected void modifyLocalChatterCount (ChatChannel channel, int delta)
    {
        int newcount = getLocalChatterCount(channel) + delta;
        if (newcount > 0) { 
            _subscriberCounts.put(channel, newcount);
        } else {
            _subscriberCounts.remove(channel);
        }
    }
 
    protected class HostedChannelListener extends SetAdapter
    {
        public HostedChannelListener (ChatChannelObject ccobj) {
            _ccobj = ccobj;
        }
        public void entryRemoved (EntryRemovedEvent event) {
            if (ChatChannelObject.CHATTERS.equals(event.getName())) {
                if (_ccobj.chatters.size() == 0) {
                    _ccobj.removeListener(this);
                    shutdownHostedChannel(_ccobj);
                }
            }
        }
        protected ChatChannelObject _ccobj;
    };

    protected class SubscriptionChannelListener extends SetAdapter
    {
        public SubscriptionChannelListener (ChatChannelObject ccobj) {
            _ccobj = ccobj;
        }
        public void entryRemoved (EntryRemovedEvent event) {
            if (getLocalChatterCount(_ccobj.channel) <= 0) {
                _ccobj.removeListener(this);
                shutdownSubscriptionChannel(_ccobj);
            }
        }
        protected ChatChannelObject _ccobj;
    };
    
    protected abstract class ChannelFinalizer implements ResultListener<ChatChannelObject>
    {
        public ChannelFinalizer (final MemberObject user, final ChatChannel channel,
                                 final ChatChannelService.ResultListener listener)
        {
            _user = user;
            _channel = channel;
            _listener = listener;
        }

        public void requestFailed (String cause)
        {
            _listener.requestFailed(cause);
        }
        
        public void requestFailed (Exception cause)
        {
            log.log(Level.WARNING, "Chat channel finalizer failed " +
                    "[user=" + _user.who() + ", channel=" + _channel + "].", cause);
            _listener.requestFailed(ChatChannelCodes.E_INTERNAL_ERROR);
        }

        public void requestCompleted (ChatChannelObject ccobj)
        {
            requestCompleted(ccobj, _listener);
        }
        
        public abstract void requestCompleted (ChatChannelObject ccobj, 
                                               ChatChannelService.ResultListener listener);

        protected MemberObject _user;
        protected ChatChannel _channel;
        protected ChatChannelService.ResultListener _listener;
    };

    protected class ChannelModificationListener implements PeerChatService.ConfirmListener
    {
        public ChannelModificationListener (
            ChatterInfo userInfo, ChatChannel channel, int localChatterDelta)
        {
            _userInfo = userInfo;
            _channel = channel;
            _delta = localChatterDelta;
        }
        
        public void requestProcessed () {
            if (_delta != 0) {
                modifyLocalChatterCount(_channel, _delta);
            }
            log.info("Channel subscription modification successful [channel=" + _channel +
                     ", user=" + _userInfo + "].");
        }
        public void requestFailed (String cause) {
            log.info("Channel subscription modification failed [channel=" + _channel +
                     ", user=" + _userInfo + ", cause = " + cause + "].");
        }

        protected ChatterInfo _userInfo;
        protected ChatChannel _channel;
        protected int _delta;
    };
    
    /** Contains a mapping of all chat channels we know about, hosted or subscribed. */
    protected HashMap<ChatChannel, ChatChannelObject> _knownChannels =
        new HashMap<ChatChannel, ChatChannelObject>();

    /** For subscribed channels only, maps from channel info to the number of clients
     *  on this peer that are chatting on that channel. */
    protected HashMap<ChatChannel, Integer> _subscriberCounts =
        new HashMap<ChatChannel, Integer>();
}
