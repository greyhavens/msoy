package com.threerings.msoy.client {

import flash.events.Event;

import mx.controls.TextArea;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

/**
 * IMPORTANT NOTE: this class was written for testing things and does not
 * necessarily represent a valid starting point for writing the chat
 * widget we'll eventually need.
 */
public class ChatTextArea extends TextArea
    implements ChatDisplay
{
    public function ChatTextArea (ctx :MsoyContext)
    {
        _ctx = ctx;
        this.editable = false;

        // TODO
        width = 400;
        height = 150;

        // set up some events to manage how we'll be shown, etc.
        addEventListener("creationComplete", checkVis);
        addEventListener("show", checkVis);
        addEventListener("hide", checkVis);
    }

    // documentation inherited from interface ChatDisplay
    public function clear () :void
    {
        this.htmlText = "";
    }

    // documentation inherited from interface ChatDisplay
    public function displayMessage (msg :ChatMessage) :void
    {
        if (!_scrollBot) {
            _scrollBot = (verticalScrollPosition == maxVerticalScrollPosition);
        }

        // display the message
        if (msg is UserMessage) {
            this.htmlText += "<font color=\"red\">&lt;" +
                (msg as UserMessage).speaker + "&gt;</font> ";
        }
        this.htmlText += msg.message;
    }

    // documentation inherited
    override protected function updateDisplayList (uw :Number, uh :Number) :void
    {
        super.updateDisplayList(uw, uh);

        if (_scrollBot) {
            verticalScrollPosition = maxVerticalScrollPosition;
            _scrollBot = false;
        }
    }

    /**
     * Check to see if we should register or unregister ourselves as a
     * ChatDisplay.
     */
    protected function checkVis (event :Event) :void
    {
        var chatdir :ChatDirector = _ctx.getChatDirector();
        if (this.visible) {
            chatdir.addChatDisplay(this);
        } else {
            chatdir.removeChatDisplay(this);
        }
    }

    /** The giver of life. */
    protected var _ctx :MsoyContext;

    protected var _scrollBot :Boolean;
}
}
