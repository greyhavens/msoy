// 
// $Id$

package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.flex.CommandMenu;

import com.whirled.ui.NameLabel;
import com.whirled.ui.NameLabelCreator;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.VizMemberName;

public class MsoyNameLabelCreator 
    implements NameLabelCreator
{
    public function MsoyNameLabelCreator (mctx :MsoyContext) 
    {
        _mctx = mctx;
    }

    public function createLabel (name :Name) :NameLabel
    {
        if (!(name is VizMemberName)) {
            log.warning("MsoyNameLabelCreator only supports creating labels for VizMemberNames [" + 
                name + "]");
            return null;
        }
        
        var vizName :VizMemberName = name as VizMemberName;
        var labelBox :LabelBox = new LabelBox(vizName);
        labelBox.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
            handlePlayerClicked(vizName);
        });
        return labelBox;
    }
    
    protected function handlePlayerClicked (name :VizMemberName) :void
    {
        var menuItems :Array = [];
        _mctx.getMsoyController().addMemberMenuItems(name, menuItems);
        var menu :CommandMenu = CommandMenu.createMenu(menuItems);
        menu.setDispatcher(_mctx.getTopPanel());
        menu.popUpAtMouse();
    }

    private static const log :Log = Log.getLog(MsoyNameLabelCreator);

    protected var _mctx :MsoyContext;
}
}

import flash.text.TextFieldAutoSize;

import mx.containers.HBox;

import mx.core.ScrollPolicy;

import com.threerings.util.Log;

import com.whirled.ui.NameLabel;
import com.whirled.ui.PlayerList;

import com.threerings.flex.FlexWrapper;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.data.VizMemberName;

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.world.client.NameField;

class LabelBox extends HBox
    implements NameLabel
{
    public function LabelBox (name :VizMemberName)
    {
        _name = name;

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        setStyle("borderThickness", 0);
        setStyle("borderStyle", "none");
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

    protected var _name :VizMemberName;
    protected var _label :NameField;
}
