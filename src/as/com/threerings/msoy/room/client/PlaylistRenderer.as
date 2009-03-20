//
// $Id$

package com.threerings.msoy.room.client {

import mx.containers.HBox;
import mx.controls.Label;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.item.data.all.Audio;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.RoomObject;

public class PlaylistRenderer extends HBox
{
    public var wctx :WorldContext;
    public var roomObj :RoomObject;

    override public function set data (value :Object) :void
    {
        super.data = value;
        if (value == null) {
            return;
        }

        var audio :Audio = Audio(value);
        _name.text = audio.name;
        _name.toolTip = audio.description;

        var isPlayingNow :Boolean = (roomObj.currentSongId == audio.itemId);
        _name.setStyle("fontWeight", isPlayingNow ? "bold" : "normal");

        var canRemove :Boolean = wctx.getMsoyController().canManagePlace() ||
            (wctx.getMyId() == audio.ownerId);
        FlexUtil.setVisible(_removeBtn, canRemove);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _name = FlexUtil.createLabel(null);
        _name.percentWidth = 100;
        addChild(_name);

        _removeBtn = new CommandButton(null, doRemove);
        _removeBtn.styleName = "closeButton";
        addChild(_removeBtn);
    }

    protected function doRemove () :void
    {
        roomObj.roomService.modifyPlaylist(wctx.getClient(), Audio(data).itemId, false,
            wctx.confirmListener(null, MsoyCodes.WORLD_MSGS));
    }

    protected var _name :Label;
    protected var _removeBtn :CommandButton;
}
}
