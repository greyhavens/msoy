//
// $Id$

package com.threerings.msoy.chat.server;

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
        _ccobj = MsoyServer.omgr.registerObject(new ChatChannelObject());

        // and advertise to other peers that we're hosting this channel
        HostedChannel hosted = new HostedChannel(_channel, _ccobj.getOid());
        ((MsoyNodeObject) MsoyServer.peerMan.getNodeObject()).addToHostedChannels(hosted);

        initializeCCObj(_ccobj, _channel, new EntryListener());
        cccont.creationSucceeded(this);
    }

    // from abstract class ChannelWrapper 
    public void shutdown (SetAdapter adapter)
    {
        log.info("Shutting down hosted chat channel: " + _channel + ".");
        MsoyNodeObject host = (MsoyNodeObject) MsoyServer.peerMan.getNodeObject();
        host.removeFromHostedChannels(HostedChannel.getKey(_channel));
        deinitializeCCObj(_ccobj, adapter);
        MsoyServer.omgr.destroyObject(_ccobj.getOid());
    }

    // from abstract class ChannelWrapper 
    public void addChatter (ChatterInfo userInfo)
    {
        try {
            _mgr.addUser(null, userInfo, _channel, new ChatterListener(userInfo));
        } catch (Exception ex) {
            log.warning("Host failed to add a new user [user=" + userInfo +
                        ", channel=" + _channel + ", error=" + ex.getMessage() + "].");
        }
    }

    // from abstract class ChannelWrapper 
    public void removeChatter (ChatterInfo userInfo)
    {
        try {
            _mgr.removeUser(null, userInfo, _channel, new ChatterListener(userInfo));
        } catch (Exception ex) {
            log.warning("Host failed to remove a user [user=" + userInfo +
                        ", channel=" + _channel + ", error=" + ex.getMessage() + "].");
        }
    }

    /**
     * Listens to chatter removal events, and deletes the channel if nobody is
     * participating in it anymore.
     */
    protected class EntryListener extends SetAdapter
    {
        public void entryRemoved (EntryRemovedEvent event) {
            if (ChatChannelObject.CHATTERS.equals(event.getName())) {
                if (_ccobj.chatters.size() == 0) {
                    shutdown(this);
                    _mgr.removeWrapper(HostedWrapper.this);
                }
            }
        }
    };

    /**
     * Called when chatter list is changed, it fills logs with marginally useful debug info. :)
     */
    protected class ChatterListener implements PeerChatService.ConfirmListener
    {
        public ChatterListener (ChatterInfo userInfo) {
            _userInfo = userInfo;
        }
        public void requestProcessed () {
            log.info("Channel subscription modification successful [channel=" +
                     _channel + ", user=" + _userInfo + "].");
        }
        public void requestFailed (String cause) {
            log.info("Channel subscription modification failed [channel=" +
                     _channel + ", user=" + _userInfo + ", cause = " + cause + "].");
        }
        protected ChatterInfo _userInfo;
    };
}
