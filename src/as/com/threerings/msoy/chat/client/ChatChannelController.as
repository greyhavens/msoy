//
// $Id$

package com.threerings.msoy.chat.client {

import com.whirled.ui.PlayerList;

import com.threerings.util.ExpiringSet;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.Util;
import com.threerings.util.ValueEvent;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.presents.util.SafeSubscriber;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.SystemMessage;

import com.threerings.msoy.ui.MsoyNameLabelCreator;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.VizMemberName;

import com.threerings.msoy.data.all.JabberName;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.client.ChatChannelService;

import com.threerings.msoy.chat.data.ChannelMessage;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;

/**
 * Controlles the connection to the channel object, including reconnecting when switching servers
 */
public class ChatChannelController
    implements SetListener, AttributeChangeListener, Subscriber
{
    public function ChatChannelController (ctx :MsoyContext, channel :ChatChannel, 
        showTabFn :Function = null)
    {
        _ctx = ctx;
        _channel = channel;
        _showTabFn = showTabFn;

        if (_channel.type != ChatChannel.MEMBER_CHANNEL &&
                _channel.type != ChatChannel.JABBER_CHANNEL) {
            _occList = new ChannelPlayerList(
                new MsoyNameLabelCreator(ctx, (_channel.type == ChatChannel.ROOM_CHANNEL)));

            _departing = new ExpiringSet(3);
            _departing.addEventListener(ExpiringSet.ELEMENT_EXPIRED, handleDeparted);

            _ccsvc = _ctx.getClient().requireService(ChatChannelService) as ChatChannelService;
            connect();
        }
    }

    public function get channel () :ChatChannel
    {
        return _channel;
    }

    public function get occupantList () :PlayerList
    {
        return _occList;
    }

    public function connect () :void
    {
        if (_channel.type == ChatChannel.MEMBER_CHANNEL ||
            _channel.type == ChatChannel.JABBER_CHANNEL) {
            return;
        }

        if (_isConnecting) {
            log.warning("Asked to connect to a channel while still attempting to connect! [" + 
                _channel + "]");
            Log.dumpStack();
            return;
        }

        if (!_isConnected) {
            _ccsvc.joinChannel(
                _ctx.getClient(), _channel, new ResultWrapper(failed, gotChannelOid));
            _isConnecting = true;
        }
    }

    public function disconnect () :void
    {
        if (_isConnected) {
            if (_ccsub != null) {
                _ccsub.unsubscribe(_ctx.getClient().getDObjectManager());
                _ccsub = null;
            }
            if (_ccobj != null) {
                _ctx.getMsoyChatDirector().removeAuxiliarySource(_ccobj);
                _ccobj = null;
            }
            _ccsvc.leaveChannel(_ctx.getClient(), channel);
            _isConnected = false;
        }
    }

    public function shutdown () :void
    {
        disconnect();
        _isShutdown = true;
    }

    // from Subscriber 
    public function objectAvailable (obj :DObject) :void
    {
        _ccobj = (obj as ChatChannelObject);
        _ctx.getMsoyChatDirector().addAuxiliarySource(_ccobj, _channel.toLocalType());
        if (_showTabFn != null) {
            _showTabFn();
        }

        _ccobj.addListener(this);

        redispatchMissedMessages();

        if (_occList != null) {
            _occList.clear();
            for each (var chatter :VizMemberName in _ccobj.chatters.toArray()) {
                _occList.addItem(chatter);
            }
        }
    }

    // from Subscriber
    public function requestFailed (oid :int, cause :ObjectAccessError) :void
    {
        failed(cause.message);
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == ChatChannelObject.CHATTERS && _occList != null) {
            var chatter :VizMemberName = (event.getEntry() as VizMemberName);

            // did the departing chatter come back? if so, just remove them from the expiring set
            if (_departing.contains(chatter)) {
                _departing.remove(chatter);
                return;
            }

            // if I just saw myself entering the channel, ignore the event
            if (Util.equals(chatter, _ctx.getMyName())) {
                return;
            }

            _occList.addItem(chatter);
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == ChatChannelObject.CHATTERS) {
            _occList.itemUpdated(event.getEntry() as VizMemberName);
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == ChatChannelObject.CHATTERS) {
            _departing.add(event.getOldEntry());
        }
    }

    // from interface AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == ChatChannelObject.CHANNEL) {
            // channel changes on the ChatChannelObject should be purely cosmetic.  It should
            // not change the ChatChannel's hashCode(), or it's toLocalType().  
            _channel = event.getValue() as ChatChannel;

            // It can, however, change the visual name of the channel, and the text in the tab
            // should be changed accordingly.
            var chatTabs :ChatTabBar = _ctx.getTopPanel().getHeaderBar().getChatTabs();
            if (chatTabs != null) {
                chatTabs.setName(_channel.toLocalType(), _channel.ident.toString());
            }
        }
    }

    /**
     * Formats the string as a message from this controller's channel, and sends it on to the
     * chat director in the appropriate way.
     */
    public function sendChat (message :String) :void
    {
        if (_channel.type == ChatChannel.MEMBER_CHANNEL) {
            _ctx.getChatDirector().requestTell(
                _channel.ident as Name, message, null, _channel.toLocalType());

        } else if (_channel.type == ChatChannel.JABBER_CHANNEL) {
            _ctx.getMsoyChatDirector().requestJabber(_channel.ident as JabberName, message,
                _channel.toLocalType());

        } else {
            var result :String =
                _ctx.getChatDirector().requestChat(_ccobj.speakService, message, false);
            if (result != ChatCodes.SUCCESS) {
                displayFeedback(result);
            }
        }
    }

    protected function gotChannelOid (result :Object) :void
    {
        _isConnecting = false;
        _isConnected = true;
        if (_isShutdown) {
            // zoiks! we got hsutdown before we got our channel oid, just leave
            disconnect();
        } else {
            _ccsub = new SafeSubscriber(int(result), this);
            _ccsub.subscribe(_ctx.getClient().getDObjectManager());
        }
    }

    protected function failed (cause :String) :void
    {
        _isConnecting = false;
        var msg :String = MessageBundle.compose("m.join_channel_failed", cause);
        _ctx.displayFeedback(MsoyCodes.CHAT_MSGS, msg);
    }

    protected function displayFeedback (message :String) :void
    {
        var msg :SystemMessage = new SystemMessage(
            message, MsoyCodes.CHAT_MSGS, SystemMessage.FEEDBACK);
        _ctx.getChatDirector().dispatchMessage(msg, _channel.toLocalType());
    }

    protected function handleDeparted (event :ValueEvent) :void
    {
        if (_occList != null) {
            _occList.removeItem(event.value as MemberName);
        }
    }

    protected function redispatchMissedMessages () :void
    {
        var recentMessageCount :int = _ccobj.recentMessages.length;
        var missedMessages :Array = new Array();
        var history :HistoryList = _ctx.getMsoyChatDirector().getHistoryList();

        // find the last chat message for this channel that this client knows about
        var lastHistoryMessage :ChannelMessage = null;
        for (var ii :int = history.size() - 1; ii >= 0; ii--) {
            lastHistoryMessage = history.get(ii) as ChannelMessage;
            if (lastHistoryMessage != null && 
                lastHistoryMessage.localtype == _channel.toLocalType()) {
                break;
            }
            lastHistoryMessage = null;
        }

        // now try to find it in the server's recent history. looking backwards from newest to
        // olders, remember all messages up to the one we've already seen.
        for (ii = _ccobj.recentMessages.length - 1; ii >= 0; ii--) {
            var serverMessage :ChannelMessage = _ccobj.recentMessages[ii];
            // compare by timestamp - since those have millisecond resolution, there's minimal
            // chance of false positives. also, if history is empty, just store all server messages
            if (lastHistoryMessage != null &&
                lastHistoryMessage.creationTime.equals(serverMessage.creationTime)) {
                break;
            } else {
                missedMessages.push(serverMessage);
            }
        }

        // we have them all - redispatch on this channel
        while (missedMessages.length > 0) {
            var msg :ChannelMessage = missedMessages.pop() as ChannelMessage;
            _ctx.getChatDirector().dispatchMessage(msg, _channel.toLocalType());
        }
    }

    private static const log :Log = Log.getLog(ChatChannelController);

    protected var _ctx :MsoyContext;
    protected var _showTabFn :Function;
    protected var _isShutdown :Boolean = false;
    protected var _isConnected :Boolean = false;
    protected var _isConnecting :Boolean = false;

    protected var _ccsvc :ChatChannelService;
    protected var _ccsub :SafeSubscriber;
    protected var _ccobj :ChatChannelObject;

    /** Queue of DepartureInfo objects, holding on to those recently departed. */
    protected var _departing :ExpiringSet;

    protected var _channel :ChatChannel;
    protected var _occList :PlayerList;
}
}
