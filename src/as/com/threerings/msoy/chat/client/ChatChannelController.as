//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.Util;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;

import com.threerings.msoy.client.BaseContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.data.ChannelMessage;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.ChatterInfo;

/**
 * Controlles the chat that gets displayed for a given chat channel.
 */
public class ChatChannelController
    implements SetListener, MessageListener
{
    public function ChatChannelController (ctx :BaseContext, channel :ChatChannel, 
        history :HistoryList)
    {
        _ctx = ctx;
        _channel = channel;
        _history = history;

        _departing = new ExpiringSet(3.0, handleDeparted);
    }

    public function init (ccobj :ChatChannelObject, serverSwitch :Boolean = false) :void
    {
        if (ccobj != null) {
            _ccobj = ccobj;
            _ccobj.addListener(this);

            redispatchMissedMessages();
            if (!serverSwitch) {
                // report on the current occupants of the channel
                var occs :String = "";
                for each (var ci :ChatterInfo in _ccobj.chatters.toArray()) {
                    if (occs.length > 0) {
                        occs += ", ";
                    }
                    occs += ci.name;
                }
                displayFeedback(MessageBundle.tcompose("m.channel_occs", occs));
            }                 
        }
    }

    public function get channel () :ChatChannel
    {
        return _channel;
    }

    public function displayChat () :void
    {
        var overlay :ChatOverlay = _ctx.getTopPanel().getChatOverlay();
        if (overlay != null) {
            overlay.setHistory(_history);
        }
    }

    public function shutdown () :void
    {
        if (_ccobj != null) {
            _ccobj.removeListener(this);
            _ccobj = null;
        }
    }

    public function reinit (ccobj :ChatChannelObject) :void
    {
        if (ccobj != _ccobj) {
            shutdown();
            init(ccobj, true);
        }
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == ChatChannelObject.CHATTERS) {
            var ci :ChatterInfo = (event.getEntry() as ChatterInfo);

            // did the departing chatter come back? if so, just remove them from the expiring set
            if (_departing.contains(ci.name)) {
                _departing.remove(ci.name);
                return;
            }

            // if I just saw myself entering the channel, ignore the event
            if (Util.equals(ci.name, _ctx.getMyName())) {
                return;
            }

            // someone new just entered. display a message.
            displayFeedback(MessageBundle.tcompose("m.channel_entered", ci.name));           
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == ChatChannelObject.CHATTERS) {
            var ci :ChatterInfo = (event.getOldEntry() as ChatterInfo);
            _departing.add(ci.name);
        }
    }

    // from interface MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        // todo: react to the ChannelChat messages
    }

    public function sendChat (message :String) :void
    {
        if (_channel.type == ChatChannel.MEMBER_CHANNEL) {
            _ctx.getChatDirector().requestTell(_channel.ident as Name, message, null);

        } else {
            var result :String =
                _ctx.getChatDirector().requestChat(_ccobj.speakService, message, false);
            if (result != ChatCodes.SUCCESS) {
                displayFeedback(result);
            }
        }
    }

    public function addMessage (msg :ChatMessage) :void
    {
        // if our history is currently being displayed somewhere, it will take care of getting
        // this message to that display.
        _history.addMessage(msg);
    }

    protected function displayFeedback (message :String) :void
    {
        var msg :SystemMessage = new SystemMessage(
            message, MsoyCodes.CHAT_MSGS, SystemMessage.FEEDBACK);
        _ctx.getChatDirector().dispatchMessage(msg, _channel.toLocalType());
    }

    protected function handleDeparted (name :MemberName) :void
    {
        displayFeedback(MessageBundle.tcompose("m.channel_left", name));
    }

    protected function redispatchMissedMessages () :void
    {
        var recentMessageCount :int = _ccobj.recentMessages.length;
        var missedMessages :Array = new Array();

        // find the last chat message this client knows about
        var lastHistoryMessage :ChannelMessage = null;
        for (var hc :int = _history.size(), hi :int = hc - 1; hi >= 0; hi--) {
            lastHistoryMessage = _history.get(hi) as ChannelMessage;
            if (lastHistoryMessage != null) {
                break;
            }
        }
        
        // now try to find it in the server's recent history. looking backwards from newest to
        // olders, remember all messages up to the one we've already seen.
        for (var ii :int = _ccobj.recentMessages.length - 1; ii >= 0; ii--) {
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

    protected var _ctx :BaseContext;

    /** A reference to our chat channel object if we're a non-friend channel. */
    protected var _ccobj :ChatChannelObject;

    /** Queue of DepartureInfo objects, holding on to those recently departed. */
    protected var _departing :ExpiringSet;

    protected var _channel :ChatChannel;
    protected var _history :HistoryList;
}
}
