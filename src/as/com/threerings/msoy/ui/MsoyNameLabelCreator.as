// 
// $Id$

package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import mx.containers.HBox;

import mx.controls.Label;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.flex.CommandMenu;
import com.threerings.flex.NameLabelCreator;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.VizMemberName;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.MediaDesc;

public class MsoyNameLabelCreator implements NameLabelCreator
{
    public function MsoyNameLabelCreator (mctx :MsoyContext) 
    {
        _mctx = mctx;
    }

    public function createLabel (name :Name) :UIComponent
    {
        var labelBox :HBox = new HBox();
        labelBox.verticalScrollPolicy = ScrollPolicy.OFF;
        labelBox.horizontalScrollPolicy = ScrollPolicy.OFF;
        labelBox.setStyle("borderThickness", 0);
        labelBox.setStyle("borderStyle", "none");
        if (name is VizMemberName) {
            labelBox.addChild(MediaWrapper.createView(
                (name as VizMemberName).getPhoto(), MediaDesc.QUARTER_THUMBNAIL_SIZE));
        }
        var label :Label = new Label();
        label.text = "" + name;
        var memberName :MemberName = name as MemberName;
        if (memberName != null) {
            labelBox.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
                handlePlayerClicked(memberName);
            });
        }
        labelBox.addChild(label);
        return labelBox;
    }
    
    protected function handlePlayerClicked (name :MemberName) :void
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
