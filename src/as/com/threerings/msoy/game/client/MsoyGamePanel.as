//
// $Id$

package com.threerings.msoy.game.client {

import flash.geom.Rectangle;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.client.EZGamePanel;
import com.threerings.ezgame.client.GameControlBackend;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.HistoryList;
import com.threerings.msoy.game.data.MsoyGameObject;

import com.threerings.flash.MediaContainer;

import com.threerings.util.ValueEvent;

import mx.events.ResizeEvent;

public class MsoyGamePanel extends EZGamePanel
{
    public function MsoyGamePanel (ctx :GameContext, ctrl :MsoyGameController)
    {
        super(ctx, ctrl);

        _chatOverlay = new ChatOverlay(ctx.getMessageManager());
    }

    /**
     * Enables or disables the chat overlay. The overlay is hidden while disabled.
     */
    public function setChatEnabled (enabled :Boolean) :void
    {
        if (enabled) {
            _ctx.getChatDirector().addChatDisplay(_chatOverlay);
        } else {
            _ctx.getChatDirector().removeChatDisplay(_chatOverlay);
        }
        _chatOverlay.setTarget(enabled ? this : null);
        (_ctx as GameContext).getTopPanel().getControlBar().setChatEnabled(enabled);
    }

    /**
     * Relocates and resizes the chat overlay.
     */
    public function setChatBounds (bounds :Rectangle) :void
    {
        _chatOverlay.setTargetBounds(bounds);
    }

    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _gameView.addEventListener(ResizeEvent.RESIZE, handleGameContainerResize);
        _gameView.getMediaContainer().addEventListener(
            MediaContainer.SIZE_KNOWN, handleMediaContainerResize);

        resizeChatOverlay();
        setChatEnabled(true);
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _gameView.getMediaContainer().removeEventListener(
            MediaContainer.SIZE_KNOWN, handleMediaContainerResize);
        _gameView.removeEventListener(ResizeEvent.RESIZE, handleGameContainerResize);
        _ctx.getChatDirector().removeChatDisplay(_chatOverlay);
        _chatOverlay.setTarget(null);

        super.didLeavePlace(plobj);

        // reenable chat
        (_ctx as GameContext).getTopPanel().getControlBar().setChatEnabled(true);
    }

    override protected function createBackend () :GameControlBackend
    {
        return new WhirledGameControlBackend(
            (_ctx as GameContext), (_ezObj as MsoyGameObject), (_ctrl as MsoyGameController));
    }

    protected function handleMediaContainerResize (event :ValueEvent) :void
    {
        resizeChatOverlay ();
    }

    protected function handleGameContainerResize (event :ResizeEvent) :void
    {
        resizeChatOverlay ();
    }

    protected function resizeChatOverlay () :void
    {
        var h :Number = 0;
        var media :MediaContainer = _gameView.getMediaContainer();
        if (media != null) {
            // chat box should fit between the game media, and the bottom of the container
            h = (this.height - media.getContentHeight()) / this.height;
        }
        // in any case, chat window should be at least 100px tall
        h = Math.max(h, 100 / this.height);

        _chatOverlay.setSubtitlePercentage(h);
    }

    /** Overlays chat on top of the game. */
    protected var _chatOverlay :ChatOverlay;
}
}
