package com.threerings.msoy.world.client {

import mx.containers.TitleWindow;

import mx.managers.PopUpManager;

import com.threerings.mx.controls.CommandButton;

import com.threerings.msoy.client.MsoyContext;

/**
 * A floating control widget that aids in scene editing.
 */
public class EditRoomPanel extends TitleWindow
{
    public function EditRoomPanel (
        ctx :MsoyContext, ctrl :EditRoomHelper, roomView :RoomView)
    {
        _ctx = ctx;
        _ctrl = ctrl;
        showCloseButton = false;
        title = ctx.xlate("editing", "t.editing");

        PopUpManager.addPopUp(this, roomView);
    }

    public function popDown () :void
    {
        PopUpManager.removePopUp(this);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var btn :CommandButton =
            new CommandButton(EditRoomHelper.INSERT_PORTAL);
        btn.label = _ctx.xlate("editing", "b.new_portal");
        addChild(btn);

        btn = new CommandButton(EditRoomHelper.SAVE_EDITS);
        btn.label = _ctx.xlate("editing", "b.save_edits");
        addChild(btn);

        btn = new CommandButton(EditRoomHelper.DISCARD_EDITS);
        btn.label = _ctx.xlate("editing", "b.discard_edits");
        addChild(btn);
    }

    protected var _ctx :MsoyContext;

    protected var _ctrl :EditRoomHelper;
}
}
