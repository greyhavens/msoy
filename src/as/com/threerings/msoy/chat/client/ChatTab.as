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

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.chat.data.MsoyChatChannel

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.all.MemberName;

[Style(name="buttonSkin", type="Class", inherit="no")]
[Event(name="tabClick", type="flash.events.Event")]
[Event(name="tabCloseClick", type="flash.events.Event")]

public class ChatTab extends HBox
{
    /** Event that gets fired when this tab is clicked */
    public static const TAB_CLICK :String = "tabClick";

    /** Event that gets fired when this tab's close box is clicked */
    public static const TAB_CLOSE_CLICK :String = "tabCloseClick";

    public static const SELECTED :int = 1;
    public static const UNSELECTED :int = 2;
    public static const ATTENTION :int = 3;

    public function ChatTab (
        ctx :MsoyContext, bar :ChatTabBar, channel :MsoyChatChannel, roomName :String = null)
    {
        _ctx = ctx;
        _bar = bar;
        _channel = channel;

        addChild(_label = new Label());
        _label.styleName = this;
        _label.maxWidth = 175;
        _label.text = roomName;

        // close button is not added until displayCloseBox(true) is called
        _closeButton = new Canvas();
        _closeButton.width = 13;
        _closeButton.height = 13;

        height = HeaderBar.getHeight(ctx.getMsoyClient());

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

    public function get channel () :MsoyChatChannel
    {
        return _channel;
    }

    public function get localtype () :String
    {
        return (_channel != null) ? _channel.toLocalType() : ChatCodes.PLACE_CHAT_TYPE;
    }

    /**
     * This is not a setter, because setters are FUCKING EVIL.
     */
    public function setChannel (channel :MsoyChatChannel) :void
    {
        _channel = channel;
        text = channel.ident.toString();
    }

    public function getChannelType () :int
    {
        return (_channel != null) ? _channel.type : MsoyChatChannel.ROOM_CHANNEL;
    }

    public function isSpeakableChannel () :Boolean
    {
        return (_channel != null) && _channel.isSpeakable();
    }

    /**
     * Returns to the member id of the person that this tab is a tell tab for, if it is.  If it's
     * not, returns 0.
     */
    public function getTellMemberId () :int
    {
        if (getChannelType() == MsoyChatChannel.MEMBER_CHANNEL) {
            return (_channel.ident as MemberName).getId();
        }
        return 0;
    }

    public function displayCloseBox (display :Boolean) :void
    {
        if (!display && contains(_closeButton)) {
            removeChild(_closeButton);
        } else if (display && !contains(_closeButton)) {
            addChild(_closeButton);
        }
    }

    public function getVisualState () :int
    {
        if (styleName.indexOf("selected") == 0) {
            return SELECTED;
        } else if (styleName.indexOf("unselected") == 0) {
            return UNSELECTED;
        } else if (styleName.indexOf("attention") == 0) {
            return ATTENTION;
        } else {
            log.warning("Tab is in an unknown state [" + styleName + "]");
            return -1;
        }
    }

    public function setVisualState (state :int) :void
    {
        var style :String;
        switch (state) {
        case SELECTED:
            style = "selected";
            break;

        case UNSELECTED:
            style = "unselected";
            break;

        case ATTENTION:
            style = "attention";
            break;

        default:
            log.warning("Unknown visual state [" + state + "]");
            return;
        }
        displayShine(state == ATTENTION);
        displayCloseBox((state == SELECTED) && (_bar.chatTabIndex(this) != 0));

        switch (getChannelType()) {
        case MsoyChatChannel.ROOM_CHANNEL:
            style += "RoomTab";
            break;

        case MsoyChatChannel.GROUP_CHANNEL:
            style += "GroupTab";
            break;

        case MsoyChatChannel.JABBER_CHANNEL: // fall through to MEMBER_CHANNEL
        case MsoyChatChannel.MEMBER_CHANNEL:
            style += "TellTab";
            break;
        }

        // finally, assign the stylename
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
            _shine.setActualSize(uw, uh);
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

    private static const log :Log = Log.getLog(ChatTab);

    protected static const BUTTON_SKIN :String = "buttonSkin";
    protected static const CLOSE_SKIN :String = "closeSkin";

    protected static const PADDING :int = 5;

    protected var _label :Label;
    protected var _channel :MsoyChatChannel;
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

import flash.events.Event;

import mx.containers.Canvas;

import caurina.transitions.Tweener;

import com.threerings.util.Util;

class Shine extends Canvas
{
    public function Shine ()
    {
        styleName = "tabShine";
        alpha = 0;
        addEventListener(Event.ADDED_TO_STAGE, Util.adapt(stopGo));
    }

    protected function stopGo () :void
    {
        Tweener.removeTweens(this);
        if (parent != null) {
            var maxAlpha :Number = getStyle("maxAlpha") as Number;
            var hperiod :Number = (getStyle("period") as Number) / 2;
            Tweener.addTween(this, { alpha: maxAlpha, time: hperiod, delay: 0,
                                                transition: "easeinoutsine" });
            Tweener.addTween(this, { alpha: 0, time: hperiod, delay: hperiod,
                                                transition: "easeinoutsine", onComplete: stopGo });
        }
    }

    override public function setActualSize (uw :Number, uh :Number) :void
    {
        x = 0;
        y = 0;
        width = uw;
        height = uh;
    }
}
