//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

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

        // steal the main place box and jam it into ourbadselves
        _placeBox = _ctx.getTopPanel().takePlaceContainer();
        if (_placeBox == null) {
            Log.getLog(this).warning("Failed to get PlaceBox.");
        } else {
            addChild(_placeBox);
            addEventListener(Event.ADDED_TO_STAGE, handleAdded);
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

    protected function handleAdded (event :Event) :void
    {
        var height :int = stage.stageHeight - ControlBar.HEIGHT - HeaderBar.HEIGHT -
            TopPanel.DECORATIVE_MARGIN_HEIGHT;
        _placeBox.height = height;
        _placeBox.width = stage.stageWidth;
        _placeBox.move(0, 0);
        _placeBox.wasResized(stage.stageWidth, height);
    }

    protected var _placeBox :PlaceBox;
}
}
