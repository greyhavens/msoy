//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.MouseEvent;

import mx.containers.Canvas;
import mx.containers.HBox;

import mx.controls.Label;

import mx.core.IFlexDisplayObject;
import mx.core.UIComponent;

import com.threerings.util.Log;

import com.threerings.msoy.chat.data.ChatChannel

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.MsoyContext;

[Style(name="buttonSkin", type="Class", inherit="no")]
[Event(name="tabClick", type="flash.events.Event")]
[Event(name="tabCloseClick", type="flash.events.Event")]

public class ChatTab extends HBox
{
    /** Event that gets fired when this tab is clicked */
    public static const TAB_CLICK :String = "tabClick";

    /** Event that gets fired when this tab's close box is clicked */
    public static const TAB_CLOSE_CLICK :String = "tabCloseClick"

    public static const SELECTED :int = 1;
    public static const UNSELECTED :int = 2;
    public static const ATTENTION :int = 3;
    
    public function ChatTab (ctx :MsoyContext, bar :ChatTabBar, channel :ChatChannel, 
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

        addEventListener(MouseEvent.CLICK, tabClicked);
        _closeButton.addEventListener(MouseEvent.CLICK, closeClicked);

        _shine = new Shine();
        _shine.includeInLayout = false;
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

    public function setVisualState (state :int) :void
    {
        var style :String;
        switch (state) {
        case SELECTED: 
            style = "selected"; 
            displayShine(false);
            displayCloseBox(_controller != null);
            break;

        case UNSELECTED: 
            style = "unselected"; 
            displayShine(false);
            displayCloseBox(false);
            break;

        case ATTENTION: 
            style = "attention"; 
            displayShine(true);
            displayCloseBox(false);
            break;

        default:
            Log.getLog(this).warning("Unknown visual state [" + state + "]");
            return;
        }

        if (_controller == null || _controller.channel.type == ChatChannel.ROOM_CHANNEL) {
            style += "RoomTab";
            if (state == SELECTED) {
                _ctx.getTopPanel().getControlBar().setChatColor(COLOR_ROOM);
            }
        } else if (_controller.channel.type == ChatChannel.GROUP_CHANNEL) {
            style += "GroupTab";
            if (state == SELECTED) {
                _ctx.getTopPanel().getControlBar().setChatColor(COLOR_GROUP);
            }
        } else if (_controller.channel.type == ChatChannel.MEMBER_CHANNEL) {
            style += "TellTab";
            if (state == SELECTED) {
                _ctx.getTopPanel().getControlBar().setChatColor(COLOR_TELL);
            }
        } else {
            Log.getLog(this).warning("Unkown channel type for skinning [" + 
                _controller.channel.type + "]");
            return;
        }

        styleName = style;
    }

    protected function tabClicked (event :MouseEvent) :void
    {
        dispatchEvent(new Event(TAB_CLICK));
    }

    protected function closeClicked (event :MouseEvent) :void
    {
        // prevent tabClicked from getting called.
        event.stopImmediatePropagation();

        dispatchEvent(new Event(TAB_CLOSE_CLICK));
    }

    protected function displayShine (show :Boolean) :void
    {
        if (show && _shine.parent != this) {
            addChildAt(_shine, 0);
        } else if (!show && _shine.parent == this) {
            removeChild(_shine);
        } 

    }

    override protected function updateDisplayList (uw :Number, uh :Number) :void
    {
        super.updateDisplayList(unscaledWidth, unscaledHeight);

        updateShine(uw, uh);
        updateButtonSkin(uw, uh);
        updateCloseSkin();
    }

    protected function updateShine (uw :Number, uh :Number) :void
    {
        if (_shine.parent == this) {
            _shine.setActualSize(uw, uh / 2);
        }
    }

    protected function updateButtonSkin (uw :Number, uh :Number) :void 
    {
        var newSkin :Class = getStyle(BUTTON_SKIN) as Class;
        if (newSkin == _buttonSkinClass) {
            if (_currentSkin != null) {
                (_currentSkin as IFlexDisplayObject).setActualSize(uw, uh);
            }
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

    protected static const COLOR_ROOM :int = 0xFFFFFF;
    protected static const COLOR_TELL :int = 0xB6D2E3;
    protected static const COLOR_GROUP :int = 0xF0AF75;

    protected var _label :Label;
    protected var _controller :ChatChannelController;
    protected var _ctx :MsoyContext;
    protected var _bar :ChatTabBar;
    protected var _currentSkin :DisplayObject;
    protected var _buttonSkinClass :Class;
    protected var _closeSkin :DisplayObject;
    protected var _closeButton :Canvas;
    protected var _closeSkinClass :Class;
    protected var _shine :UIComponent;
}
}

import flash.display.DisplayObject;

import mx.containers.Canvas;

class Shine extends Canvas
{
    public function Shine () 
    {
        _shine = new ATTENTION_SHINE() as DisplayObject;
        _naturalWidth = _shine.width;
        _shine.y = 0.5;
        _shine.x = 0;
        rawChildren.addChild(_shine);
        setStyle("borderStyle", "none");
    }

    override public function setActualSize (uw :Number, uh :Number) :void
    {
        x = 0;
        y = 0;
        width = uw;
        height = uh;
        _shine.scaleX = (uw - 3) / _naturalWidth;
        _shine.x = uw / 2;
    }

    [Embed(source="../../../../../../../rsrc/media/skins/tab/tab_attention.swf#attention")]
    protected static const ATTENTION_SHINE :Class;

    protected var _shine :DisplayObject;
    protected var _naturalWidth :Number;
}
