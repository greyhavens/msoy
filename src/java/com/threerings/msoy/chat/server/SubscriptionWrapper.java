//
// $Id$

package com.threerings.msoy.chat.server;

import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.presents.client.Client;

import com.threerings.crowd.chat.server.SpeakDispatcher;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.MsoyNodeObject;

import static com.threerings.msoy.Log.log;

/**
 * Wrapper for channels hosted on another peer, and only subscribed here.
 */
public class SubscriptionWrapper extends ChannelWrapper
{
    public SubscriptionWrapper (ChatChannelManager mgr, ChatChannel channel)
    {
        super(mgr, channel);
    }

    // from abstract class ChannelWrapper
    public void initialize (final ChannelCreationContinuation cccont)
    {
        // let's create a proxy to a channel object hosted on another server
        MsoyNodeObject host = _peerMan.getChannelHost(_channel);
        HostedChannel hosted = host.hostedChannelz.get(HostedChannel.getKey(_channel));
        if (hosted == null) {
            // the host used to have this channel, but it disappeared. where did it go?
            log.warning("Remote channel no longer hosted, cannot be subscribed! " +
                        "[previous host=" + host + ", channel=" + _channel + "].");
            return;
        }

        // after we've subscribed, here's what we'll do: create a new local "channel",
        // and initialize the proxied channel distributed object.
        ResultListener<Integer> subscriptionResult = new ResultListener<Integer>() {
            public void requestCompleted (Integer localOid) {
                // subscription successful! we have the local oid of the proxy object
                _ccobj = (ChatChannelObject)_omgr.getObject(localOid);
                _ccobj.channel = _channel;
                // create our special handler, which forwards all speak requests to the host.
                // nota bene: we are creating a local instance of the special handler, and
                // clobbering the local copy of the distributed object's speakService field with
                // its reference. this means that the distributed object on this peer will have a
                // *different* value of speakService from that on other peers, including the host.
                // we need this so that our clients will always talk to the local service,
                // instead of the service initialized by the host. (todo: move this out of dobj?)
                SubscriptionWrapper superthis = SubscriptionWrapper.this;
                _ccobj.speakService = _invmgr.registerDispatcher(
                    new SpeakDispatcher(new SubscriptionSpeakHandler(_peerMan, superthis)));
                _ccobj.addListener(superthis);
                // we're so done.
                cccont.creationSucceeded(superthis);
            }
            public void requestFailed (Exception cause) {
                log.warning("Channel subscription failed [cause=" + cause.getMessage() + "].");
                cccont.creationFailed(cause.getMessage());
            }
        };

        // now let's get the proxy object - the listener will do the rest
        _peerMan.proxyRemoteObject(host.nodeName, hosted.oid, subscriptionResult);
    }

    // from abstract class ChannelWrapper
    public void shutdown ()
    {
        // clean up the object
        _ccobj.removeListener(this);
        _invmgr.clearDispatcher(_ccobj.speakService);

        // unsubscribe from the proxy
        MsoyNodeObject host = _peerMan.getChannelHost(_channel);
        if (host == null) {
            // host already destroyed the distributed object
            return;
        }

        HostedChannel hostedInfo = host.hostedChannelz.get(HostedChannel.getKey(_channel));
        _peerMan.unproxyRemoteObject(host.nodeName, hostedInfo.oid);
    }

    // from abstract class ChannelWrapper
    public void addChatter (VizMemberName chatter)
    {
        removeStaleMessagesFromHistory();

        Tuple<MsoyNodeObject,Client> bits = getChannelHost("addChatter", chatter);
        if (bits != null) {
            bits.left.peerChatService.addUser(
                bits.right, chatter, _channel, new ChatterListener(chatter, 1));
        }
    }

    // from abstract class ChannelWrapper
    public void removeChatter (VizMemberName chatter)
    {
        Tuple<MsoyNodeObject,Client> bits = getChannelHost("removeChatter", chatter);
        if (bits != null) {
            bits.left.peerChatService.removeUser(
                bits.right, chatter, _channel, new ChatterListener(chatter, -1));
        }
    }

    // from abstract class ChannelWrapper
    public void updateChatter (final VizMemberName chatter)
    {
        Tuple<MsoyNodeObject,Client> bits = getChannelHost("updateChatter", chatter);
        if (bits != null) {
            bits.left.peerChatService.updateUser(bits.right, chatter, _channel,
                new PeerChatService.InvocationListener() {
                    public void requestFailed (String cause) {
                        log.info("Update chatter failed [channel=" + _channel +
                            ", user=" + chatter + ", cause=" + cause + "].");
                    }
                });
        }
    }

    // from abstract class ChannelWrapper
    public void updateChannel (final ChatChannel channel)
    {
        if (!_channel.equals(channel)) {
            log.warning("Attempted to update ChatChannel on wrapper with incompatible channel!  " +
                        "Only cosmetic channel updates are supported. [current=" + _channel +
                        ", new=" + channel + "]");
            return;
        }

        Tuple<MsoyNodeObject,Client> bits = getChannelHost("updateChannel", null);
        if (bits != null) {
            bits.left.peerChatService.updateChannel(bits.right, channel,
                new PeerChatService.InvocationListener() {
                    public void requestFailed (String cause) {
                        log.info("Update channel failed [channel=" + channel + ", cause=" +
                            cause + "].");
                    }
                });
        }
    }

    /**
     * Gets our host node object and client, but logs a warning and returns null if either of the
     * two are missing for strange reasons.
     */
    protected Tuple<MsoyNodeObject,Client> getChannelHost (String caller, VizMemberName chatter)
    {
        MsoyNodeObject host = _peerMan.getChannelHost(_channel);
        if (host == null) {
            log.warning("No host for channel [caller=" + caller + ", chatter=" + chatter +
                ", " + _channel + "].");
            return null;
        }

        Client client = _peerMan.getPeerClient(host.nodeName);
        if (client == null) {
            log.warning("Missing client for chat host node [caller=" + caller +
                        ", node=" + host.nodeName + "].");
            return null;
        }

        return Tuple.create(host, client);
    }

    /**
     * Called when chatter list is changed, it updates the count of local chat clients
     * participating in the channel through this peer.
     */
    protected class ChatterListener implements PeerChatService.ConfirmListener
    {
        public ChatterListener (VizMemberName chatter, int delta) {
            _chatter = chatter;
            _delta = delta;
        }
        public void requestProcessed () {
            _localChatterCount += _delta;
            if (_localChatterCount <= 0) {
                shutdown();
                _mgr.removeWrapper(SubscriptionWrapper.this);
            }
        }
        public void requestFailed (String cause) {
            log.info("Subscription channel: chatter action failed [channel=" + _channel
                     + ", user=" + _chatter + ", count=" + _localChatterCount +
                     ", cause = " + cause + "].");
        }
        protected VizMemberName _chatter;
        protected int _delta;
    };

    protected int _localChatterCount;
}
