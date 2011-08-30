//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.core.ClassFactory;
import mx.core.IFactory;

import com.threerings.util.StringUtil;

import com.threerings.flex.CommandMenu;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlayerRenderer;
import com.threerings.msoy.data.all.FriendEntry;

public class FriendRenderer extends PlayerRenderer
{
    public static function createFactory (ctx :MsoyContext) :IFactory
    {
        var cf :ClassFactory = new ClassFactory(FriendRenderer);
        cf.properties = { mctx: ctx };
        return cf;
    }

    public function FriendRenderer ()
    {
        super();

        addEventListener(MouseEvent.CLICK, handleClick);
    }

    override protected function addCustomControls (content :VBox) :void
    {
        var friend :FriendEntry = FriendEntry(this.data);

        var name :Label = FlexUtil.createLabel(friend.name.toString(), "playerLabel");
        name.width = content.width;
        content.addChild(name);

        var statusContainer :HBox = new HBox();
        statusContainer.setStyle("paddingLeft", 3);

        var isMuted :Boolean = mctx.getMuteDirector().isMuted(friend.name);
        var status :Label;
        if (isMuted) {
            status = FlexUtil.createLabel(Msgs.GENERAL.get("l.muted"), "friendStatusLabelMuted");
        } else {
            var statusString :String = StringUtil.deNull(
                mctx.getChatDirector().filter(friend.status, friend.name, false));
            status = FlexUtil.createLabel(statusString, "friendStatusLabel");
        }
        status.width = content.width;
        statusContainer.addChild(status);

        content.addChild(statusContainer);
    }

    protected function handleClick (event :MouseEvent) :void
    {
        var friend :FriendEntry = this.data as FriendEntry;
        if (friend != null) {
            var menuItems :Array = [];
            mctx.getMsoyController().addMemberMenuItems(friend.name, menuItems, true);
            CommandMenu.createMenu(menuItems, mctx.getTopPanel()).popUpAtMouse();
        }
    }
}
}
