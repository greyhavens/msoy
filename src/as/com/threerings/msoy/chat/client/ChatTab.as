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

import com.threerings.msoy.chat.data.ChatChannel

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.all.MemberName;

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
        roomName :String = null)
    {
        _ctx = ctx;
        _bar = bar;
        _channel = channel;

        addChild(_label = new Label());
        _label.styleName = this;
        _label.text = roomName;

        // close button is not added until displayCloseBox(true) is called
        _closeButton = new Canvas();
        _closeButton.width = 10;
        _closeButton.height = 10;

        // check box is not added until displayCheckBox(true) is called
        _checkBox = new Canvas();
        _checkBox.width = 10;
        _checkBox.height = 10; 
        _checkBoxChecked = false;
        _checkBox.toolTip = Msgs.CHAT.get("i.keep_channel");

        height = HeaderBar.HEIGHT;

        setStyle("paddingRight", PADDING);
        setStyle("paddingLeft", PADDING);
        setStyle("verticalAlign", "middle");
        setStyle("horizontalGap", 3);

        addEventListener(MouseEvent.CLICK, tabClicked);
        _closeButton.addEventListener(MouseEvent.CLICK, closeClicked);
        _checkBox.addEventListener(MouseEvent.CLICK, checkClicked);
        _checkBox.addEventListener(MouseEvent.MOUSE_OVER, checkMouseOver);
        _checkBox.addEventListener(MouseEvent.MOUSE_OUT, checkMouseOut);

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

    public function get checked () :Boolean
    {
        return _checkBoxChecked;
    }

    public function get localtype () :String
    {
        return _channel != null ? _channel.toLocalType() : ChatCodes.PLACE_CHAT_TYPE;
    }
    
    /**
     * Returns to the member id of the person that this tab is a tell tab for, if it is.  If it's
     * not, returns 0.
     */
    public function getTellMemberId () :int
    {
    	if (_channel == null) {
    		return 0;
    	}
    	
    	return _channel.type != ChatChannel.MEMBER_CHANNEL ? 0 : 
    	   (_channel.ident as MemberName).getMemberId();
    }

    public function displayCloseBox (display :Boolean) :void
    {
        if (!display && contains(_closeButton)) {
            removeChild(_closeButton);
        } else if (display && !contains(_closeButton)) {
            addChild(_closeButton);
        }
    }

    public function displayCheckBox (display :Boolean) :void
    {
        if (!display && contains(_checkBox)) {
            removeChild(_checkBox);
        } else if (display && !contains(_checkBox)) {
            addChildAt(_checkBox, 0);
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
            displayShine(false);
            displayCloseBox(_bar.chatTabIndex(this) != 0);
            displayCheckBox(_bar.chatTabIndex(this) == 0 && _channel != null && 
                            _channel.type == ChatChannel.ROOM_CHANNEL);
            break;

        case UNSELECTED:
            style = "unselected";
            displayShine(false);
            displayCloseBox(false);
            displayCheckBox(false);
            break;

        case ATTENTION:
            style = "attention";
            displayShine(true);
            displayCloseBox(false);
            displayCheckBox(false);
            break;

        default:
            log.warning("Unknown visual state [" + state + "]");
            return;
        }

        if (_channel == null || _channel.type == ChatChannel.ROOM_CHANNEL) {
            style += "RoomTab";
            if (state == SELECTED) {
                _ctx.getTopPanel().getControlBar().setChatColor(COLOR_ROOM);
            }
        } else if (_channel.type == ChatChannel.GROUP_CHANNEL) {
            style += "GroupTab";
            if (state == SELECTED) {
                _ctx.getTopPanel().getControlBar().setChatColor(COLOR_GROUP);
            }
        } else if (_channel.type == ChatChannel.MEMBER_CHANNEL ||
                   _channel.type == ChatChannel.JABBER_CHANNEL) {
            style += "TellTab";
            if (state == SELECTED) {
                _ctx.getTopPanel().getControlBar().setChatColor(COLOR_TELL);
            }
        } else {
            log.warning("Unkown channel type for skinning [" + _channel.type + "]");
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

    protected function checkClicked (event :MouseEvent) :void
    {
        _checkBoxChecked = !_checkBoxChecked;
        if (_checkBoxChecked) {
            if (_checkUpSkin != null && _checkUpSkin.parent == _checkBox) {
                _checkBox.rawChildren.removeChild(_checkUpSkin);
            }

            if (_checkOverSkin != null && _checkOverSkin.parent == _checkBox) {
                _checkBox.rawChildren.removeChild(_checkOverSkin);
            }

            if (_checkSelectedSkin != null) {
                _checkBox.rawChildren.addChild(_checkSelectedSkin);
                (_checkSelectedSkin as IFlexDisplayObject).setActualSize(_checkBox.width,
                                                                         _checkBox.height);
            }
        } else {
            if (_checkSelectedSkin != null && _checkSelectedSkin.parent == _checkBox) {
                _checkBox.rawChildren.removeChild(_checkSelectedSkin);
            }

            if (_checkUpSkin != null) {
                _checkBox.rawChildren.addChild(_checkUpSkin);
                (_checkUpSkin as IFlexDisplayObject).setActualSize(_checkBox.width, 
                                                                   _checkBox.height);
            }
        }
    }

    protected function checkMouseOver (event :MouseEvent) :void
    {
        if (_checkBoxChecked) {
            return;
        }

        if (_checkUpSkin != null && _checkUpSkin.parent == _checkBox) {
            _checkBox.rawChildren.removeChild(_checkUpSkin);
        }

        if (_checkOverSkin != null) {
            _checkBox.rawChildren.addChild(_checkOverSkin);
            (_checkOverSkin as IFlexDisplayObject).setActualSize(_checkBox.width, _checkBox.height);
        }
    }

    protected function checkMouseOut (event :MouseEvent) :void
    {
        if (_checkBoxChecked) {
            return;
        }

        if (_checkOverSkin != null && _checkOverSkin.parent == _checkBox) {
            _checkBox.rawChildren.removeChild(_checkOverSkin);
        }

        if (_checkUpSkin != null) {
            _checkBox.rawChildren.addChild(_checkUpSkin);
            (_checkUpSkin as IFlexDisplayObject).setActualSize(_checkBox.width, _checkBox.height);
        }
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
        updateCheckSkin();
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

    protected function updateCheckSkin () :void
    {
        var newUpSkin :Class = getStyle(CHECK_UP_SKIN) as Class;
        var newOverSkin :Class = getStyle(CHECK_OVER_SKIN) as Class;
        var newSelectedSkin :Class = getStyle(CHECK_SELECTED_SKIN) as Class;

        var selected :Boolean = false;
        if (newSelectedSkin != _checkSelectedSkinClass) {
            if (_checkSelectedSkin != null && _checkSelectedSkin.parent == _checkBox) {
                _checkBox.rawChildren.removeChild(_checkSelectedSkin);
                selected = true;
            }

            _checkSelectedSkinClass = newSelectedSkin;
            if (_checkSelectedSkinClass != null) {
                _checkSelectedSkin = new _checkSelectedSkinClass() as DisplayObject;
            }

            if (_checkSelectedSkin != null && (selected || _checkBoxChecked)) {
                _checkBox.rawChildren.addChild(_checkSelectedSkin);
                (_checkSelectedSkin as IFlexDisplayObject).setActualSize(_checkBox.width,
                                                                         _checkBox.height);
            }
        }

        var over :Boolean = false;
        if (newOverSkin != _checkOverSkinClass) {
            if (_checkOverSkin != null && _checkOverSkin.parent == _checkBox) {
                _checkBox.rawChildren.removeChild(_checkOverSkin);
                over = true;
            }

            _checkOverSkinClass = newOverSkin;
            if (_checkOverSkinClass != null) {
                _checkOverSkin = new _checkOverSkinClass() as DisplayObject;
            }

            if (_checkOverSkin != null && over) {
                _checkBox.rawChildren.addChild(_checkOverSkin);
                (_checkOverSkin as IFlexDisplayObject).setActualSize(_checkBox.width,
                                                                     _checkBox.height);
            }
        }

        var up :Boolean = false;
        if (newUpSkin != _checkUpSkinClass) {
            if (_checkUpSkin != null && _checkUpSkin.parent == _checkBox) {
                _checkBox.rawChildren.removeChild(_checkUpSkin);
                up = true;
            }

            _checkUpSkinClass = newUpSkin;
            if (_checkUpSkinClass != null) {
                _checkUpSkin = new _checkUpSkinClass() as DisplayObject;
            }

            if (_checkUpSkin != null && (up || (!_checkBoxChecked && !over))) {
                _checkBox.rawChildren.addChild(_checkUpSkin);
                (_checkUpSkin as IFlexDisplayObject).setActualSize(_checkBox.width,
                                                                   _checkBox.height);
            }
        }
    }

    private static const log :Log = Log.getLog(ChatTab);

    protected static const BUTTON_SKIN :String = "buttonSkin";
    protected static const CLOSE_SKIN :String = "closeSkin";
    protected static const CHECK_UP_SKIN :String = "checkboxUpSkin";
    protected static const CHECK_OVER_SKIN :String = "checkboxOverSkin";
    protected static const CHECK_SELECTED_SKIN :String = "checkboxSelectedSkin";

    protected static const PADDING :int = 5;

    protected static const COLOR_ROOM :int = 0xFFFFFF;
    protected static const COLOR_TELL :int = 0xFFCE7C;
    protected static const COLOR_GROUP :int = 0xC7DAEA;

    protected var _label :Label;
    protected var _channel :ChatChannel;
    protected var _ctx :MsoyContext;
    protected var _bar :ChatTabBar;
    protected var _currentSkin :DisplayObject;
    protected var _buttonSkinClass :Class;
    protected var _checkBox :Canvas;
    protected var _closeSkin :DisplayObject;
    protected var _closeButton :Canvas;
    protected var _closeSkinClass :Class;
    protected var _shine :UIComponent;
    protected var _checkUpSkinClass :Class;
    protected var _checkUpSkin :DisplayObject;
    protected var _checkOverSkinClass :Class;
    protected var _checkOverSkin :DisplayObject;
    protected var _checkSelectedSkinClass :Class;
    protected var _checkSelectedSkin :DisplayObject;
    protected var _checkBoxChecked :Boolean;
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
