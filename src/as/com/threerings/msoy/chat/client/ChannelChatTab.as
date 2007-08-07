//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;
import flash.events.TimerEvent;    
import flash.utils.Timer;
import flash.utils.getTimer; // function import

import com.threerings.util.ArrayUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.SystemMessage;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.ChatterInfo;

/**
 * Displays an actual chat channel.
 */
public class ChannelChatTab extends ChatTab
    implements SetListener
{
    public var channel :ChatChannel;

    public function ChannelChatTab (ctx :WorldContext, channel :ChatChannel)
    {
        super(ctx);
        this.channel = channel;

        _overlay = new ChatOverlay(ctx.getMessageManager());
        _overlay.setClickableGlyphs(true);

        _timer = new Timer(1000);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
    }

    public function init (ccobj :ChatChannelObject) :void
    {
        if (ccobj != null) {
            _ccobj = ccobj;
            _ccobj.addListener(this);

            // report on the current occupants of the channel
            var occs :String = "";
            for each (var ci :ChatterInfo in _ccobj.chatters.toArray()) {
                if (occs.length > 0) {
                    occs += ", ";
                }
                occs += ci.name;
            }
            displayFeedback(MessageBundle.tcompose("m.channel_occs", occs));

            _timer.addEventListener(TimerEvent.TIMER, handleTick);
            _timer.start();
        }
    }

    public function shutdown () :void
    {
        if (_ccobj != null) {
            _timer.stop();
            _timer.removeEventListener(TimerEvent.TIMER, handleTick);
            
            _ccobj.removeListener(this);
            _ccobj = null;
        }
    }

    public function reinit (ccobj :ChatChannelObject) :void
    {
        if (ccobj != _ccobj) {
            shutdown();
            init(ccobj);
        }
    }

    public function getOverlay () :ChatOverlay
    {
        return _overlay;
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == ChatChannelObject.CHATTERS) {
            var ci :ChatterInfo = (event.getEntry() as ChatterInfo);
            if (! _departed.contains(ci)) {
                displayFeedback(MessageBundle.tcompose("m.channel_entered", ci.name));
            }
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
            _departed.enqueue(ci);
        }
    }

    // @Override // from ChatTab
    override public function sendChat (message :String) :void
    {
        if (channel.type == ChatChannel.MEMBER_CHANNEL) {
            _ctx.getChatDirector().requestTell(channel.ident as Name, message, null);

        } else {
            var result :String =
                _ctx.getChatDirector().requestChat(_ccobj.speakService, message, false);
            if (result != ChatCodes.SUCCESS) {
                displayFeedback(result);
            }
        }
    }

    protected function displayFeedback (message :String) :void
    {
        var msg :SystemMessage = new SystemMessage(
            message, MsoyCodes.CHAT_MSGS, SystemMessage.FEEDBACK);
        _ctx.getChatDirector().dispatchMessage(msg, channel.toLocalType());
    }

    protected function handleAddRemove (event :Event) :void
    {
        if (event.type == Event.ADDED_TO_STAGE) {
            _overlay.setTarget(this, TopPanel.RIGHT_SIDEBAR_WIDTH);
        } else {
            _overlay.setTarget(null);
        }
    }

    protected function handleTick (event :TimerEvent) :void
    {
        while (_departed.ready()) {
            // get the departure log
            var di :DepartureInfo = _departed.dequeue();
            // is the "departed" chatter still in the room? 
            var returnedIndex :int = ArrayUtil.indexIf(_ccobj.chatters.toArray(), di.equals);
            if (returnedIndex == -1) {
                // this departed chatter had not returned. tell the player.
                displayFeedback(MessageBundle.tcompose("m.channel_left", di.chatter.name));
            }
        }
    }        

    /** Actually renders chat. */
    protected var _overlay :ChatOverlay;

    /** A reference to our chat channel object if we're a non-friend channel. */
    protected var _ccobj :ChatChannelObject;

    /** Queue of DepartureInfo objects, holding on to those recently departed. */
    protected var _departed :Departures = new Departures();

    /** Handles delayed notifications about chatters' departures. */
    protected var _timer :Timer;
    
}
}


import flash.utils.getTimer; // function import

import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.util.Util;

internal class DepartureInfo
{
    /** How long chatter departure information gets delayed, in milliseconds. */
    public static const DELAY :int = 2000;
    public var timestamp :int;
    public var chatter :ChatterInfo;

    public function DepartureInfo (ci :ChatterInfo)
    {
        this.timestamp = getTimer();
        this.chatter = ci;
    }

    public function ready () :Boolean
    {
        return ((getTimer() - timestamp) > DELAY);
    }

    public function equals (ci :ChatterInfo) :Boolean {
        return Util.equals(chatter.getKey(), ci.getKey());
    }
}

internal class Departures 
{
    public function empty () :Boolean
    {
        return _data.length == 0;
    }

    public function enqueue (ci :ChatterInfo) :void
    {
        _data.push(new DepartureInfo(ci));
    }

    public function dequeue () :DepartureInfo
    {
        return (empty() ? null : (_data.shift() as DepartureInfo));
    }

    public function ready () :Boolean
    {
        return (empty() ? false : (_data[0] as DepartureInfo).ready());
    }
    
    public function contains (ci :ChatterInfo) :Boolean
    {
        for each (var di :DepartureInfo in _data) {
            if (di.equals(ci)) {
                return true;
            }
        }
        return false;
    }

    protected var _data :Array = new Array(); // of DepartureInfo
}
