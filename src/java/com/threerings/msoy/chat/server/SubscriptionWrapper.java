//
// $Id$

package com.threerings.msoy.chat.server;

import com.samskivert.util.ResultListener;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

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
        MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(_channel);
        HostedChannel hosted = host.hostedChannels.get(HostedChannel.getKey(_channel));

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
                _ccobj = (ChatChannelObject) MsoyServer.omgr.getObject(localOid);
                initializeCCObj(_ccobj, _channel, new EntryListener());
                cccont.creationSucceeded(SubscriptionWrapper.this);
            }
            public void requestFailed (Exception cause) {
                log.warning("Channel subscription failed [cause=" + cause.getMessage() + "].");
                cccont.creationFailed(cause.getMessage());
            }
        };

        // now let's get the proxy object - the listener will do the rest
        MsoyServer.peerMan.proxyRemoteObject(host.nodeName, hosted.oid, subscriptionResult);
    }

    // from abstract class ChannelWrapper 
    public void shutdown (SetAdapter adapter)
    {
        // we need the hosting peer's object Id for this channel - so let's fetch it
        MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(_channel);
        HostedChannel hostedInfo = host.hostedChannels.get(HostedChannel.getKey(_channel));
        if (hostedInfo == null) {
            // it went away! 
            log.warning("Remote channel no longer hosted, cannot be unsubscribed! " +
                        "[previous host=" + host + ", channel=" + _channel + "].");
            return;
        }

        deinitializeCCObj(_ccobj, adapter);
        MsoyServer.peerMan.unproxyRemoteObject(host.nodeName, hostedInfo.oid);

    }
    
    // from abstract class ChannelWrapper 
    public void addChatter (ChatterInfo userInfo)
    {
        MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(_channel);
        host.peerChatService.addUser(
            MsoyServer.peerMan.getPeerClient(host.nodeName), userInfo, _channel,
            new ChatterListener(userInfo, 1));
    }

    // from abstract class ChannelWrapper 
    public void removeChatter (ChatterInfo userInfo)
    {
        MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(_channel);
        host.peerChatService.removeUser(
            MsoyServer.peerMan.getPeerClient(host.nodeName), userInfo, _channel,
            new ChatterListener(userInfo, -1));
    }

    /**
     * Listens to chatter removal events, and shuts down the channel if all of the local
     * chat participants have left.
     */
    protected class EntryListener extends SetAdapter
    {
        public void entryRemoved (EntryRemovedEvent event) {
            if (_localChatterCount <= 0) {
                shutdown(this);
                _mgr.removeWrapper(SubscriptionWrapper.this);
            }
        }
    };

    /**
     * Called when chatter list is changed, it updates the count of local chat clients
     * participating in the channel through this peer.
     */
    protected class ChatterListener implements PeerChatService.ConfirmListener
    {
        public ChatterListener (ChatterInfo userInfo, int delta) {
            _userInfo = userInfo;
            _delta = delta;
        }
        public void requestProcessed () {
            _localChatterCount += _delta;
            log.info("Channel subscription modification successful [channel=" +
                     _channel + ", user=" + _userInfo + "].");
        }
        public void requestFailed (String cause) {
            log.info("Channel subscription modification failed [channel=" +
                     _channel + ", user=" + _userInfo + ", cause = " + cause + "].");
        }
        protected ChatterInfo _userInfo;
        protected int _delta;
    };

    protected int _localChatterCount;
}
