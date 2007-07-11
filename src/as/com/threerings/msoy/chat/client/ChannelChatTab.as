//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

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

        _overlay = new ChatOverlay(ctx);
        _overlay.setClickableGlyphs(true);

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
        }
    }

    public function shutdown () :void
    {
        if (_ccobj != null) {
            _ccobj.removeListener(this);
            _ccobj = null;
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
            displayFeedback(MessageBundle.tcompose("m.channel_left", ci.name));
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

    /** Actually renders chat. */
    protected var _overlay :ChatOverlay;

    /** A reference to our chat channel object if we're a non-friend channel. */
    protected var _ccobj :ChatChannelObject;
}
}
