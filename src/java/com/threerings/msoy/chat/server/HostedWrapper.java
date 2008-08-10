//
// $Id$

package com.threerings.msoy.chat.server;

import com.samskivert.util.Interval;

import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.chat.server.SpeakDispatcher;

import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.MsoyNodeObject;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;

import static com.threerings.msoy.Log.log;

/**
 * Wrapper for channels hosted on this peer server.
 */
public class HostedWrapper extends ChannelWrapper
{
    public HostedWrapper (ChatChannelManager mgr, ChatChannel channel)
    {
        super(mgr, channel);
    }

    // from abstract class ChannelWrapper
    public void initialize (final ChannelCreationContinuation cccont)
    {
        log.info("Creating chat channel " + _channel + ".");

        // create and initialize a new chat channel object
        _ccobj = _omgr.registerObject(new ChatChannelObject());
        _ccobj.channel = _channel;

        // and advertise to other peers that we're hosting this channel
        HostedChannel hosted = new HostedChannel(_channel, _ccobj.getOid());
        ((MsoyNodeObject) _peerMan.getNodeObject()).addToHostedChannelz(hosted);

        // initialize speak service
        SpeakDispatcher sd = new SpeakDispatcher(new HostedSpeakHandler(this, _mgr));
        _ccobj.setSpeakService(_invmgr.registerDispatcher(sd));
        _ccobj.addListener(this);

        cccont.creationSucceeded(this);
    }

    // from abstract class ChannelWrapper
    public void shutdown ()
    {
        _ccobj.removeListener(this);
        _invmgr.clearDispatcher(_ccobj.speakService);

        log.info("Shutting down hosted chat channel: " + _channel + ".");
        MsoyNodeObject host = (MsoyNodeObject)_peerMan.getNodeObject();
        host.removeFromHostedChannelz(HostedChannel.getKey(_channel));

        // clean up the hosted dobject
        _omgr.destroyObject(_ccobj.getOid());
    }

    // from abstract class ChannelWrapper
    public void addChatter (VizMemberName chatter)
    {
        try {
            removeStaleMessagesFromHistory();
            _mgr.addUser(null, chatter, _channel, new ChatterListener(chatter));
            cancelShutdowner();
        } catch (Exception ex) {
            log.warning("Host failed to add a new user [user=" + chatter +
                        ", channel=" + _channel + ", error=" + ex.getMessage() + "].");
        }
    }

    // from abstract class ChannelWrapper
    public void removeChatter (VizMemberName chatter)
    {
        try {
            _mgr.removeUser(null, chatter, _channel, new ChatterListener(chatter));
        } catch (Exception ex) {
            log.warning("Host failed to remove a user [user=" + chatter +
                        ", channel=" + _channel + ", error=" + ex.getMessage() + "].");
        }
    }

    // from abstract class ChannelWrapper
    public void updateChatter (final VizMemberName chatter)
    {
        try {
            _mgr.updateUser(null, chatter, _channel,
                new PeerChatService.InvocationListener() {
                    public void requestFailed (String cause) {
                        log.info("Hosted Wrapper updateChatter failed [channel=" + _channel +
                            ", user=" + chatter + ", cause=" + cause + "].");
                    }
                });
        } catch (Exception ex) {
            log.warning("Host failed to update a user [user=" + chatter +
                        ", channel=" + _channel + ", error=" + ex.getMessage() + "].");
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

        try {
            _mgr.updateChannel(null, channel, new PeerChatService.InvocationListener() {
                public void requestFailed (String cause) {
                    log.info("Hosted Wrapper failed to update channel [channel=" + channel + "]");
                }
            });
        } catch (Exception ex) {
            log.warning("Host failed to update channel on ccobj [channel=" + channel + "]");
        }
    }

    /**
     * Wrapper around the distributed channel object, to let chat manager add or remove
     * users from the chatter list. It is assumed the caller already checked whether
     * this modification is valid.
     *
     * @param chatter user to be added or removed from this channel
     * @param addAction if true, the user will be added, otherwise they will be removed.  If true
     *                  and the chatter is already in the channel, and update will be done.
     */
    protected void updateDistributedObject (VizMemberName chatter, boolean addAction)
    {
        if (addAction) {
            if (_ccobj.chatters.contains(chatter)) {
                _ccobj.updateChatters(chatter);
            } else {
                _ccobj.addToChatters(chatter);
            }
        } else {
            if (_ccobj.chatters.contains(chatter)) {
                _ccobj.removeFromChatters(chatter.getKey());
                if (_ccobj.chatters.size() == 0) {
                    checkShutdownInterval();
                }
            } else {
                log.warning("Requested to remove non-member of chatter set [channel=" + _channel +
                            ", chatter=" + chatter + "].");
            }
        }
    }

    /**
     * Updates the channel attribute on the ChatChannelObject.  It is assumed that the caller has
     * checked to make sure that the hashCode() and toLocalType() of this new channel are the
     * same as the previous channel attribute.  This should only be used to make cosmetic changes
     * to the ChatChannel on the object (room name change, etc).
     */
    protected void updateChannelOnObject (ChatChannel channel)
    {
        _ccobj.setChannel(channel);
    }

    protected void checkShutdownInterval ()
    {
        // queue up a shutdown interval, unless we've already got one.
        if (_shutdownInterval == null) {
            _shutdownInterval = new Interval((PresentsDObjectMgr)_ccobj.getManager()) {
                public void expired () {
                    _mgr.removeWrapper(HostedWrapper.this);
                    shutdown();
                }
            };
            _shutdownInterval.schedule(SHUTDOWN_PERIOD);
        }
    }

    /**
     * Cancels any registered shutdown interval.
     */
    protected void cancelShutdowner ()
    {
        if (_shutdownInterval != null) {
            _shutdownInterval.cancel();
            _shutdownInterval = null;
        }
    }

    /**
     * Called when chatter list is changed, deletes the channel if nobody is
     * participating in it anymore.
     */
    protected class ChatterListener implements PeerChatService.ConfirmListener
    {
        public ChatterListener (VizMemberName chatter) {
            _chatter = chatter;
        }
        public void requestProcessed () {
        }
        public void requestFailed (String cause) {
            log.info("Hosted channel: chatter action failed [channel=" + _channel +
                     ", user=" + _chatter + ", chatterCount=" +
                     _ccobj.chatters.size() + ", cause = " + cause + "].");
        }
        protected VizMemberName _chatter;
    };

    protected static final long SHUTDOWN_PERIOD = 5 * 60 * 1000L;

    protected Interval _shutdownInterval;
}
