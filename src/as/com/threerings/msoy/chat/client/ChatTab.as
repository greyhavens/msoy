//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObject;

import mx.containers.Canvas;
import mx.containers.HBox;

import mx.controls.Label;

import mx.core.IFlexDisplayObject;

import com.threerings.msoy.chat.data.ChatChannel

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.WorldContext;

[Style(name="buttonSkin", type="Class", inherit="no")]

public class ChatTab extends HBox
{
    public function ChatTab (ctx :WorldContext, bar :ChatTabBar, channel :ChatChannel, 
        history :HistoryList, roomName :String = null)
    {
        _ctx = ctx;
        _bar = bar;
        if (channel != null) {
            _controller = new ChatChannelController(ctx, channel, history);
            if (roomName == null) { 
                roomName = "" + channel.ident;
            }
        }
        addChild(_label = new Label());
        _label.styleName = this;
        _label.text = roomName;

        // close button is not added until displayCloseBox(true) is called
        _closeButton = new Canvas();
        _closeButton.width = 10;
        _closeButton.height = 10;

        height = HeaderBar.HEIGHT;

        setStyle("paddingRight", PADDING);
        setStyle("paddingLeft", PADDING);
        setStyle("verticalAlign", "middle");
        setStyle("horizontalGap", 3);
    }

    public function set text (label :String) :void
    {
        _label.text = label;
    }

    public function get text () :String
    {
        return _label.text;
    }

    public function get controller () :ChatChannelController
    {
        return _controller;
    }

    public function displayCloseBox (display :Boolean) :void
    {
        if (!display && contains(_closeButton)) {
            removeChild(_closeButton);
        } else if (display && !contains(_closeButton)) {
            addChild(_closeButton);
        }
    }

    public function displayChat () :void
    {
        if (_controller != null) {
            _controller.displayChat();
        } else {
            var overlay :ChatOverlay = _ctx.getTopPanel().getChatOverlay();
            if (overlay != null) {
                overlay.setHistory(_bar.getLocationHistory());
            }
        }
    }

    override protected function updateDisplayList (uw :Number, uh :Number) :void
    {
        super.updateDisplayList(unscaledWidth, unscaledHeight);

        updateButtonSkin(uw, uh);
        updateCloseSkin();
    }

    protected function updateButtonSkin (uw :Number, uh :Number) :void 
    {
        var newSkin :Class = getStyle(BUTTON_SKIN) as Class;
        if (newSkin == _buttonSkinClass) {
            return;
        }

        if (_currentSkin != null) {
            rawChildren.removeChild(_currentSkin);
        }

        _buttonSkinClass = newSkin;
        if (_buttonSkinClass != null) {
            _currentSkin = new _buttonSkinClass() as DisplayObject;
        }

        if (_currentSkin != null) {
            rawChildren.addChildAt(_currentSkin, 0);
            (_currentSkin as IFlexDisplayObject).setActualSize(uw, uh);
        }
    }

    protected function updateCloseSkin () :void
    {
        var newSkin :Class = getStyle(CLOSE_SKIN) as Class;
        if (newSkin == _closeSkinClass) {
            return;
        }

        if (_closeSkin != null) {
            _closeButton.rawChildren.removeChild(_closeSkin);
        }

        _closeSkinClass = newSkin;
        if (_closeSkinClass != null) {
            _closeSkin = new _closeSkinClass() as DisplayObject;
        }

        if (_closeSkin != null) {
            _closeButton.rawChildren.addChild(_closeSkin);
            (_closeSkin as IFlexDisplayObject).setActualSize(_closeButton.width, 
                                                             _closeButton.height);
        }
    }
    
    protected static const BUTTON_SKIN :String = "buttonSkin";
    protected static const CLOSE_SKIN :String = "closeSkin";

    protected static const PADDING :int = 5;

    protected var _label :Label;
    protected var _controller :ChatChannelController;
    protected var _ctx :WorldContext;
    protected var _bar :ChatTabBar;
    protected var _currentSkin :DisplayObject;
    protected var _buttonSkinClass :Class;
    protected var _closeSkin :DisplayObject;
    protected var _closeButton :Canvas;
    protected var _closeSkinClass :Class;
}
}
