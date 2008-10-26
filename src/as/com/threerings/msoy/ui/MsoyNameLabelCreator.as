// 
// $Id$

package com.threerings.msoy.ui {

import com.threerings.util.Log;
import com.threerings.util.Name;

import com.whirled.ui.NameLabel;
import com.whirled.ui.NameLabelCreator;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.VizMemberName;

public class MsoyNameLabelCreator 
    implements NameLabelCreator
{
    public function MsoyNameLabelCreator (mctx :MsoyContext, forRoom :Boolean = false)
    {
        _mctx = mctx;
        _forRoom = forRoom;
    }

    public function createLabel (name :Name) :NameLabel
    {
        if (!(name is VizMemberName)) {
            Log.getLog(this).warning("MsoyNameLabelCreator only supports creating labels " +
                "for VizMemberNames [" + name + "]");
            return null;
        }
        
        return new LabelBox(_mctx, name as VizMemberName, _forRoom);
    }

    protected var _mctx :MsoyContext;

    protected var _forRoom :Boolean;
}
}

import flash.events.MouseEvent;

import flash.text.TextFieldAutoSize;

import mx.containers.HBox;

import mx.core.ScrollPolicy;

import com.threerings.util.Log;

import com.whirled.ui.NameLabel;
import com.whirled.ui.PlayerList;

import com.threerings.flex.CommandMenu;
import com.threerings.flex.FlexWrapper;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.room.client.NameField;
import com.threerings.msoy.room.client.RoomObjectView;

class LabelBox extends HBox
    implements NameLabel
{
    public function LabelBox (mctx :MsoyContext, name :VizMemberName, forRoom :Boolean)
    {
        _mctx = mctx;
        _name = name;
        _forRoom = forRoom;

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        setStyle("borderThickness", 0);
        setStyle("borderStyle", "none");
        mouseEnabled = false;

        // but the mouse is still enable on some children..
        addEventListener(MouseEvent.CLICK, handleClick);
        addEventListener(MouseEvent.ROLL_OVER, handleRoll);
        addEventListener(MouseEvent.ROLL_OUT, handleRoll);
    }

    public function setStatus (status :String) :void
    {
        // translate the PlayerList status into an OccupantInfo status
        var occStatus :int;
        switch (status) {
        default:
            Log.dumpStack();
            // but fall through to STATUS_NORMAL

        case PlayerList.STATUS_NORMAL:
        case PlayerList.STATUS_UNINITIALIZED:
            occStatus = OccupantInfo.ACTIVE;
            break;

        case PlayerList.STATUS_IDLE:
            occStatus = OccupantInfo.IDLE;
            break;

        case PlayerList.STATUS_GONE:
            occStatus = OccupantInfo.DISCONNECTED;
            break;
        }

        // and show uninitialized-ness with italics
        var italicize :Boolean = (status == PlayerList.STATUS_UNINITIALIZED);

        _label.setStatus(occStatus, italicize);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(MediaWrapper.createView(_name.getPhoto(), MediaDesc.QUARTER_THUMBNAIL_SIZE));

        _label = new NameField();
        _label.autoSize = TextFieldAutoSize.LEFT;
        _label.text = "" + _name;
        addChild(new FlexWrapper(_label));
    }
    
    protected function handleClick (event :MouseEvent) :void
    {
        var menuItems :Array = [];
        _mctx.getMsoyController().addMemberMenuItems(_name, menuItems, _forRoom);
        CommandMenu.createMenu(menuItems, _mctx.getTopPanel()).popUpAtMouse();
    }

    protected function handleRoll (event :MouseEvent) :void
    {
        var view :Object = _mctx.getPlaceView();
        if (view is RoomObjectView) {
            (view as RoomObjectView).getRoomObjectController().setHoverName(
                _name, (event.type == MouseEvent.ROLL_OVER));
        }
    }

    protected var _mctx :MsoyContext;
    protected var _name :VizMemberName;
    protected var _forRoom :Boolean;
    protected var _label :NameField;
}
