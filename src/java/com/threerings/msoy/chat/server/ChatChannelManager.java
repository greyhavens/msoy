//
// $Id$

package com.threerings.msoy.chat.server;

import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.server.SpeakUtil;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.persist.SceneRepository;

import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.peer.server.PeerChatDispatcher;
import com.threerings.msoy.peer.server.PeerChatProvider;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RoomName;

import com.threerings.msoy.chat.client.ChatChannelService;
import com.threerings.msoy.chat.data.ChannelMessage;
import com.threerings.msoy.chat.data.ChatChannel;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.RoomManager;

import static com.threerings.msoy.Log.log;

/**
 * Manages the server side of our chat channel services.
 */
@Singleton @EventThread
public class ChatChannelManager
    implements ChatChannelProvider, PeerChatProvider
{
    @Inject public ChatChannelManager (InvocationManager invmgr, ClientManager clmgr)
    {
        // register our global invocation service
        invmgr.registerDispatcher(new ChatChannelDispatcher(this), MsoyCodes.WORLD_GROUP);

        // monitor player disconnects
        clmgr.addClientObserver(new ClientManager.ClientObserver() {
            public void clientSessionDidEnd (PresentsClient client) {
                ClientObject cobj = client.getClientObject();
                if (cobj instanceof MemberObject) {
                    // remove the client from all channels
                    removeChatter((MemberObject)cobj);
                }
            }
            public void clientSessionDidStart (PresentsClient client) {
                // no op. perhaps reinstate recently disconnected clients?
            }
        });
    }

    /**
     * Initializes this manager during server startup.
     */
    public void init ()
    {
        // register and initialize our peer chat service
        MsoyNodeObject me = (MsoyNodeObject)_peerMan.getNodeObject();
        me.setPeerChatService(_invmgr.registerDispatcher(new PeerChatDispatcher(this)));
    }

    // from interface ChatChannelProvider
    public void joinChannel (ClientObject caller, final ChatChannel channel,
                             final ChatChannelService.ResultListener listener)
        throws InvocationException
    {
        final MemberObject member = (MemberObject)caller;

        // ensure this member has access to this channel
        switch (channel.type) {
        case ChatChannel.GROUP_CHANNEL:
            final GroupName gName = (GroupName) channel.ident;
            GroupMembership gm = member.groups.get(gName.getGroupId());
            if (gm != null) {
                // we're already members, no need to check further
                break;
            }

            // else check that the group is public
            _invoker.postUnit(new PersistingUnit("joinChannel", listener) {
                public void invokePersistent () throws Exception {
                    GroupRecord gRec = _groupRepo.loadGroup(gName.getGroupId());
                    _policy = (gRec == null) ? 0 : gRec.policy;
                }
                public void handleSuccess () {
                    if (_policy == Group.POLICY_PUBLIC) {
                        resolveAndJoinChannel(member, channel, listener);
                    } else {
                        log.warning("Unable to join non-public channel [member=" + member.who() +
                                    ", channel=" + channel + "]");
                        listener.requestFailed(ChatCodes.E_ACCESS_DENIED);
                    }
                }
                protected String getFailureMessage () {
                     return "Unable to load group [group=" + gName + "]";
                }
                protected int _policy;
            });
            return;

        case ChatChannel.PRIVATE_CHANNEL:
            // TODO: access controls
            break;

        case ChatChannel.ROOM_CHANNEL:
            final int sceneId = ((RoomName) channel.ident).getSceneId();
            // in most cases, the channel and the scene will be hosted on the same server, so
            // we can just pull the scene out of the scene manager and ask it about permissions
            RoomManager roomMgr = (RoomManager)_sceneReg.getSceneManager(sceneId);
            if (roomMgr != null) {
                String error = roomMgr.ratifyBodyEntry(member);
                if (error != null) {
                    throw new InvocationException(error);
                }
                // otherwise, they're clear!

            } else {
                // scene isn't resolved here, need to load up the MsoySceneModel and ask it
                // TODO: if the channel is on a different server than the room, the user
                // may now be circumventing a boot. This needs to be looked into.
                _invoker.postUnit(new PersistingUnit("joinChannel", listener) {
                    public void invokePersistent () throws Exception {
                        MsoySceneModel model = (MsoySceneModel)_sceneRepo.loadSceneModel(sceneId);
                        if (model != null) {
                            _hasRights = member.canEnterScene(
                                model.sceneId, model.ownerId, model.ownerType, model.accessControl);
                        }
                    }
                    public void handleSuccess () {
                        if (_hasRights) {
                            resolveAndJoinChannel(member, channel, listener);
                        } else {
                            log.warning("Unable to join channel due to access restrictions " +
                                "[member=" + member + ", channel=" + channel + "]");
                            listener.requestFailed(ChatCodes.E_ACCESS_DENIED);
                        }
                    }
                    protected String getFailureMessage () {
                        return "Unable to load scene model [sceneId=" + sceneId + "]";
                    }
                    protected boolean _hasRights = false;
                });
                return;
            }
            break;

        default:
            log.warning("Member requested to join invalid channel [who=" + member.who() +
                        ", channel=" + channel + "].");
            throw new InvocationException(ChatCodes.E_INTERNAL_ERROR);
        }

        // if we made it this far, we can do our joining immediately
        resolveAndJoinChannel(member, channel, listener);
    }

    // from interface ChatChannelProvider
    public void leaveChannel (ClientObject caller, ChatChannel channel)
    {
        MemberObject user = (MemberObject)caller;
        ChannelWrapper wrapper = _wrappers.get(channel);
        if (wrapper != null && wrapper.ready()) {
            log.info("Removing " + user.who() + " from " + wrapper.getChannel() + " [hosted=" +
                (wrapper instanceof HostedWrapper) + "].");
            removeChatter(wrapper, user); // this will also clean up the channel if needed
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
    public void forwardSpeak (ClientObject caller, Name chatter, ChatChannel channel,
                              String message, byte mode,
                              ChatChannelService.ConfirmListener listener)
        throws InvocationException
    {
        ChannelWrapper wrapper = _wrappers.get(channel);
        if (wrapper == null || ! wrapper.ready()) {
            listener.requestFailed("Channel not found or not initialized. [" + channel + "]");
            return;
        }

        // all that's left is sending the chat message out to everybody
        ChannelMessage msg = new ChannelMessage(chatter, message, mode, wrapper.getChannel());
        SpeakUtil.sendMessage(wrapper.getCCObj(), msg);

        listener.requestProcessed();
    }

    // from interface PeerChatProvider
    public void addUser (ClientObject caller, VizMemberName chatter, ChatChannel channel,
                         PeerChatService.ConfirmListener listener)
        throws InvocationException
    {
        ChannelWrapper wrapper = _wrappers.get(channel);
        if (wrapper instanceof HostedWrapper && wrapper.ready()) {
            ((HostedWrapper)wrapper).updateDistributedObject(chatter, true);
            listener.requestProcessed();
            return;
        }

        listener.requestFailed("Channel not found or not initialized. [" + channel + "]");
    }

    // from interface PeerChatProvider
    public void removeUser (ClientObject caller, VizMemberName chatter, ChatChannel channel,
                            PeerChatService.ConfirmListener listener)
        throws InvocationException
    {
        ChannelWrapper wrapper = _wrappers.get(channel);
        if (wrapper instanceof HostedWrapper && wrapper.ready()) {
            ((HostedWrapper)wrapper).updateDistributedObject(chatter, false);
            listener.requestProcessed();
            return;
        }

        listener.requestFailed("Channel not found or not initialized. [" + channel + "]");
    }

    // from interface PeerChatProvider
    public void updateUser (ClientObject caller, VizMemberName chatter, ChatChannel channel,
                            PeerChatService.InvocationListener listener)
        throws InvocationException
    {
        ChannelWrapper wrapper = _wrappers.get(channel);
        if (wrapper instanceof HostedWrapper && wrapper.ready()) {
            ((HostedWrapper)wrapper).updateDistributedObject(chatter, true);
            return;
        }

        listener.requestFailed("Channel not found or not initialized. [" + channel + "]");
    }

    // from interface PeerChatProvider
    public void updateChannel (ClientObject caller, ChatChannel channel,
                               PeerChatService.InvocationListener listener)
        throws InvocationException
    {
        ChannelWrapper wrapper = _wrappers.get(channel);
        if (wrapper instanceof HostedWrapper && wrapper.ready()) {
            ((HostedWrapper)wrapper).updateChannelOnObject(channel);
            return;
        }

        listener.requestFailed("Channel not found or not initialized. [" + channel + "]");
    }

    /**
     * Enumerate all the existing chat channels, both subscribed and hosted.
     */
    public Iterable<ChatChannel> getChatChannels ()
    {
        return _wrappers.keySet();
    }

    /**
     * Updates the user's VizMemberName on all subscribed channels.
     */
    public void updateMemberOnChannels (VizMemberName chatter)
    {
        for (ChannelWrapper wrapper : _wrappers.values()) {
            if (wrapper.hasMember(chatter)) {
                wrapper.updateChatter(chatter);
            }
        }
    }

    /**
     * Update the channel for a room's name.
     *
     * @param room A RoomName to look for.  It should reference the same sceneId as a currently
     *             hosted room.
     */
    public void updateRoomChannelName (RoomName room)
    {
        ChatChannel roomChannel = ChatChannel.makeRoomChannel(room);
        ChannelWrapper wrapper = _wrappers.get(roomChannel);
        if (wrapper == null) {
            log.warning(
                "Channel for locally realized room not found for name update! [" + room + "]");
            return;
        }

        wrapper.updateChannel(roomChannel);
    }

    /**
     * Creates or subscribes to the specified channel, and adds the player.
     */
    protected void resolveAndJoinChannel (final MemberObject user, final ChatChannel channel,
                                          final ChatChannelService.ResultListener listener)
    {
        withResolvedChannel(channel, new ChannelCreationContinuation(user, channel, listener) {
            public void creationSucceeded (ChannelWrapper wrapper) {
                // keep a copy of the wrapper
                addWrapper(wrapper);
                // now that we have the channel, try to add the user
                addChatter(wrapper, _user);
                // and let the caller know that they're good to go
                _listener.requestProcessed(wrapper.getCCObj().getOid());
            }
        });
    }

    /**
     * Ensures that the channel exists and is available (either by subscribing to it,
     * or by creating it from scratch). When the channel is ready, calls the listener.
     */
    protected void withResolvedChannel (final ChatChannel channel,
                                        final ChannelCreationContinuation cont)
    {
        // check if we already have it
        ChannelWrapper wrapper = _wrappers.get(channel);
        if (wrapper != null) {
            // we already have this channel, either because we're hosting it, or because
            // we're subscribed to the real host. let the caller know.
            cont.creationSucceeded(wrapper);
            return;
        }

        // we don't have the channel. is it already hosted on another server?
        MsoyNodeObject host = _peerMan.getChannelHost(channel);
        if (host != null) {
            log.info("Subscribing to another host peer: " + host.nodeName);
            SubscriptionWrapper ch = new SubscriptionWrapper(ChatChannelManager.this, channel);
            ch.initServices(_omgr, _invmgr, _peerMan);
            ch.initialize(cont);
            return;
        }

        // nobody has this channel. let's create and host one.
        NodeObject.Lock lock = MsoyPeerManager.getChannelLock(channel);
        PeerManager.LockedOperation createOp = new PeerManager.LockedOperation() {
            public void run () {
                log.info("Got lock, creating channel " + channel);
                HostedWrapper ch = new HostedWrapper(ChatChannelManager.this, channel);
                ch.initServices(_omgr, _invmgr, _peerMan);
                ch.initialize(cont);
            }
            public void fail (String peerName) {
                if (peerName != null) {
                    cont.creationFailed("Another peer got the lock: " + peerName);
                    // todo: we should try subscribing to the peer's new channel, not just give up
                }
            }
        };
        _peerMan.performWithLock(lock, createOp);
    }

    /**
     * Used by channel wrappers to add themselves to this manager.
     */
    protected void addWrapper (ChannelWrapper wrapper)
    {
        _wrappers.put(wrapper.getChannel(), wrapper);
    }

    /**
     * Used by channel wrappers to remove themselves from this manager.
     * This only changes the manager's list; channel object cleanup happens elsewhere.
     */
    protected void removeWrapper (ChannelWrapper wrapper)
    {
        ChannelWrapper removed = _wrappers.remove(wrapper.getChannel());
        if (removed != wrapper) {
            log.warning("Removed an unexpected channel [expected=" + wrapper +
                        ", removed=" + removed + "].");
        }
    }

    /**
     * Adds a new participants to a resolved channel object.
     */
    protected void addChatter (ChannelWrapper wrapper, MemberObject user)
    {
        if (wrapper.hasMember(user)) {
            log.warning("User already in chat channel, cannot add [user=" + user.who() +
                        ", channel=" + wrapper.getChannel() + "].");
            return;
        }

        wrapper.addChatter(user.memberName);
    }

    /**
     * Removes participant from a channel. Removal may trigger channel cleanup.
     */
    protected void removeChatter (ChannelWrapper wrapper, MemberObject user)
    {
        if (!wrapper.hasMember(user)) {
            log.warning("User not in chat channel, cannot remove [user=" + user.who() +
                        ", channel=" + wrapper.getChannel() + "].");
            return;
        }

        wrapper.removeChatter(user.memberName);
    }

    /**
     * Removes participant from all channels. Removal may trigger channel cleanup.
     */
    protected void removeChatter (MemberObject user)
    {
        // do this in two phases because removeChatter() can result in the wrapper itself being
        // removed which would then break our iterator due to concurrent modification
        ArrayList<ChannelWrapper> toRemove = new ArrayList<ChannelWrapper>();
        for (ChannelWrapper wrapper : _wrappers.values()) {
            if (wrapper.hasMember(user.memberName)) {
                toRemove.add(wrapper);
            }
        }
        for (ChannelWrapper wrapper : toRemove) {
            wrapper.removeChatter(user.memberName);
        }

        if (toRemove.size() > 0) {  // just for testing
            log.info("Chatter was removed from all channels [user=" + user.who() +
                     ", count=" + toRemove.size() + "].");
        }
    }

    /** Contains a mapping of all chat channels we know about, hosted or subscribed. */
    protected Map<ChatChannel, ChannelWrapper> _wrappers = Maps.newHashMap();

    // our dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected InvocationManager _invmgr;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected SceneRepository _sceneRepo;
    @Inject protected SceneRegistry _sceneReg;
}
