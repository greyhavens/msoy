//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.events.ResizeEvent;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.MsoyPlaceView;
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
            return;
        }
        _placeBox.addEventListener(MouseEvent.CLICK, mouseClickedOnRoom);

        _placeBox.mouseChildren = false;

        addChildAt(_placeBox, 0);
        addEventListener(Event.ADDED_TO_STAGE, checkSizes);
        addEventListener(ResizeEvent.RESIZE, checkSizes);
    }

    override public function setVisible (value :Boolean, noEvent :Boolean = false) :void
    {
        if (_placeBox != null) {
            var o :Object = _placeBox.getPlaceView();
            if (o is MsoyPlaceView) {
                MsoyPlaceView(o).setIsShowing(value);
            }
        }
        super.setVisible(value, noEvent);
    }

    /**
     * Called when we're removed from the display because the client is no longer minimized.
     */
    public function shutdown () :void
    {
        if (_placeBox != null) {
            _placeBox.removeEventListener(MouseEvent.CLICK, mouseClickedOnRoom);
            _placeBox.mouseChildren = true;

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
        if (stage == null) {
            return; // sometimes RESIZE comes in before ADDED_TO_STAGE
        }

        var width :int = stage.stageWidth - _ctx.getTopPanel().getLeftPanelWidth();
        var height :int = stage.stageHeight - ControlBar.HEIGHT - HeaderBar.HEIGHT -
            TopPanel.DECORATIVE_MARGIN_HEIGHT;
        // we don't have our proper width yet (the stage has not yet resized), so we have to
        // hardcode the width to the one we know we'll be receiving
        _placeBox.width = width;
        _placeBox.height = height / 2;
        _placeBox.move(0, 0);
        _placeBox.wasResized(width, height / 2);

        _chatContainer.width = width;
        _chatContainer.height = height / 2;
        _chatContainer.move(0, height/2);
    }

    protected function mouseClickedOnRoom (event :MouseEvent) :void
    {
        // if we're minimized, then request to be unminimized
        if (_ctx.getWorldClient().isMinimized()) {
            _ctx.getWorldClient().restoreClient();
            return;
        }

        // if a left panel is open and we're minimized that way, close it and unminimize
        _ctx.getTopPanel().clearLeftPanel(null);
    }

    protected var _chatContainer :ChatContainer;
    protected var _placeBox :PlaceBox;
}
}
