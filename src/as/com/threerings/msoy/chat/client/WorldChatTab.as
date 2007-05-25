//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

import mx.events.ResizeEvent;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;

/**
 * Displays the world in a little window and routes chat to the current location when this tab is
 * selected.
 */
public class WorldChatTab extends ChatTab
{
    public function WorldChatTab (ctx :WorldContext)
    {
        super(ctx);

        _chatContainer = new ChatContainer(ctx);
        addChild(_chatContainer);

        // steal the main place box and jam it into ourbadselves
        _placeBox = _ctx.getTopPanel().takePlaceContainer();
        if (_placeBox == null) {
            Log.getLog(this).warning("Failed to get PlaceBox.");
        } else {
            addChildAt(_placeBox, 0);
            addEventListener(Event.ADDED_TO_STAGE, checkSizes);
            addEventListener(ResizeEvent.RESIZE, checkSizes);
        }
    }

    /**
     * Called when we're removed from the display because the client is no longer minimized.
     */
    public function shutdown () :void
    {
        if (_placeBox != null) {
            removeChild(_placeBox);
            _ctx.getTopPanel().restorePlaceContainer();
        }
    }

    // @Override // from ChatTab
    override public function sendChat (message :String) :void
    {
        var result :String = _ctx.getChatDirector().requestChat(null, message, true);
        if (result != ChatCodes.SUCCESS) {
            _ctx.displayFeedback(null, result);
        }
    }

    protected function checkSizes (... ignored) :void
    {
        var height :int = stage.stageHeight - ControlBar.HEIGHT - HeaderBar.HEIGHT -
            TopPanel.DECORATIVE_MARGIN_HEIGHT;
        // we don't have our proper width yet (the stage has not yet resized), so we have to
        // hardcode the width to the one we know we'll be receiving
        _placeBox.width = TopPanel.RIGHT_SIDEBAR_WIDTH;
        _placeBox.height = height / 2;
        _placeBox.move(0, 0);
        _placeBox.wasResized(TopPanel.RIGHT_SIDEBAR_WIDTH, height);

        _chatContainer.width = TopPanel.RIGHT_SIDEBAR_WIDTH;
        _chatContainer.height = height / 2;
        _chatContainer.move(0, height/2);
    }

    protected var _chatContainer :ChatContainer;

    protected var _placeBox :PlaceBox;
}
}
