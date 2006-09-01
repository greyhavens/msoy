package com.threerings.msoy.world.client {

import mx.controls.Button;

import com.threerings.mx.controls.CommandButton;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.FloatingPanel;

/**
 * A floating control widget that aids in scene editing.
 */
public class EditRoomPanel extends FloatingPanel
{
    public static const DISCARD_BUTTON :int = 10;
    public static const SAVE_BUTTON :int = 11;

    public function EditRoomPanel (
        ctx :MsoyContext, ctrl :EditRoomHelper, roomView :RoomView)
    {
        super(ctx, ctx.xlate("editing", "t.editing"));
        _ctrl = ctrl;
        showCloseButton = false;

        // open non-modal with the room as the parent
        open(false, roomView);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var btn :CommandButton;

        btn = new CommandButton(EditRoomHelper.INSERT_PORTAL);
        btn.label = _ctx.xlate("editing", "b.new_portal");
        addChild(btn);

        btn = new CommandButton(EditRoomHelper.INSERT_FURNI);
        btn.label = _ctx.xlate("editing", "b.new_furni");
        addChild(btn);

        addButtons(DISCARD_BUTTON, SAVE_BUTTON);
    }

    override protected function createButton (buttonId :int) :Button
    {
        var btn :CommandButton;
        switch (buttonId) {
        case DISCARD_BUTTON:
            btn = new CommandButton(EditRoomHelper.DISCARD_EDITS);
            btn.label = _ctx.xlate("editing", "b.discard_edits");
            return btn;

        case SAVE_BUTTON:
            btn = new CommandButton(EditRoomHelper.SAVE_EDITS);
            btn.label = _ctx.xlate("editing", "b.save_edits");
            return btn;

        default:
            return super.createButton(buttonId);
        }
    }

    protected var _ctrl :EditRoomHelper;
}
}
