//
// $Id$

package com.threerings.msoy.chat.server;

import com.samskivert.util.ResultListener;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.server.SpeakDispatcher;
import com.threerings.crowd.chat.server.SpeakHandler;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObject;

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
                _ccobj.channel = _channel;
                // create our special handler, which forwards all speak requests to the host.
                // nota bene: we are creating a local instance of the special handler, and
                // clobbering the local copy of the distributed object's speakService field with
                // its reference. this means that the distributed object on this peer will have a
                // *different* value of speakService from that on other peers, including the host.
                // we need this so that our clients will always talk to the local service,
                // instead of the service initialized by the host. (todo: move this out of dobj?)
                SpeakDispatcher sd = new SpeakDispatcher(makePeerSpeakHandler());
                _ccobj.speakService = (SpeakMarshaller)MsoyServer.invmgr.registerDispatcher(sd);
                // we're so done.
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
    public void shutdown ()
    {
        // unsubscribe from the proxy
        MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(_channel);
        if (host == null) {
            // host already destroyed the distributed object
            return;
        }
        
        HostedChannel hostedInfo = host.hostedChannels.get(HostedChannel.getKey(_channel));

        MsoyServer.invmgr.clearDispatcher(_ccobj.speakService);
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
     * Helper function to create a speak handler which forwards speak requests to the hosting
     * peer, instead of pushing them through the distributed object's local speak service.
     * This handler should not be registered as the speak service on the distributed object.
     */
    protected SpeakHandler makePeerSpeakHandler ()
    {
        SpeakHandler.SpeakerValidator validator = new SpeakHandler.SpeakerValidator() {
            public boolean isValidSpeaker (DObject speakObj, ClientObject speaker, byte mode) {
                if (! (speaker instanceof MemberObject)) {
                    return false;
                }
                MemberObject member = (MemberObject)speaker;
                String errmsg = member.checkAccess(ChatCodes.CHAT_ACCESS, null);
                return speakObj == _ccobj && hasMember(member) && errmsg == null;
            }
        };
        return new SpeakHandler(_ccobj, validator){
            public void speak (ClientObject caller, String message, byte mode) {
                // ensure that the speaker is valid
                if (mode == ChatCodes.BROADCAST_MODE ||
                    !_validator.isValidSpeaker(_speakObj, caller, mode)) {
                    log.warning("Refusing invalid speak request [caller=" + caller.who() +
                                ", speakObj=" + _speakObj.which() +
                                ", message=" + message + ", mode=" + mode + "].");
                } else {
                    // ask the hosting server to speak
                    MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(_channel);
                    ChatterInfo chatter = new ChatterInfo((MemberObject)caller);
                    host.peerChatService.forwardSpeak(
                        MsoyServer.peerMan.getPeerClient(host.nodeName),
                        chatter, _channel, message, mode, new ReportListener(chatter));
                }
            }
        };
    }
    
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
            if (_localChatterCount <= 0) {
                shutdown();
                _mgr.removeWrapper(SubscriptionWrapper.this);
            }
        }
        public void requestFailed (String cause) {
            log.info("Subscription channel: chatter action failed [channel=" + _channel
                     + ", user=" + _userInfo.name + ", count=" + _localChatterCount +
                     ", cause = " + cause + "].");
        }
        protected ChatterInfo _userInfo;
        protected int _delta;
    };

    /**
     * No-op, error-reporting listener
     */
    protected class ReportListener implements PeerChatService.ConfirmListener
    {
        public ReportListener (ChatterInfo userInfo) {
            _userInfo = userInfo;
        }
        public void requestProcessed () {
            // nothing to do
        }
        public void requestFailed (String cause) {
            log.info("Subscription channel: channel action failed [channel=" + _channel
                     + ", user=" + _userInfo.name + ", cause = " + cause + "].");
        }
        protected ChatterInfo _userInfo;
    };

    protected int _localChatterCount;
}
