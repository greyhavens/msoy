//
// $Id$

package com.threerings.msoy.client {

import mx.controls.Button;
import mx.controls.TextInput;

import com.threerings.util.StringUtil;

import com.threerings.mx.controls.CommandButton;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.client.InventoryList;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Avatar;

public class PrefsDialog extends FloatingPanel
{
    public function PrefsDialog (ctx :MsoyContext)
    {
        super(ctx, Msgs.GENERAL.get("t.prefs"));
        open(true);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var memberObj :MemberObject = _ctx.getClientObject();

        var grid :Grid = new Grid();
        grid.addRow(
            MsoyUI.createLabel(Msgs.GENERAL.get("l.display_name")),
            _name = new TextInput());
        _name.text = memberObj.memberName.toString();

        addChild(grid);

        _avatars = new InventoryList(_ctx, Item.AVATAR, true);
        _avatars.dragEnabled = false;

        // TODO: this doesn't actually work because flash only does
        // reference equality on non-primitives..
        _avatars.selectedItem = memberObj.avatar;

        addChild(_avatars);

        var btn :CommandButton = new CommandButton(MsoyController.PURCHASE_ROOM);
        btn.label = Msgs.GENERAL.get("b.purchase_room");
        addChild(btn);

        addButtons(OK_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        var memberObj :MemberObject = _ctx.getClientObject();

        // save any changed info
        var newName :String = StringUtil.trim(_name.text);
        if (memberObj.memberName.toString() !== newName) {
            _ctx.getMemberDirector().setDisplayName(newName);
        }
        var newAvatar :Avatar = (_avatars.selectedItem as Avatar);
        if (newAvatar != null && !newAvatar.equals(memberObj.avatar)) {
            _ctx.getMemberDirector().setAvatar(newAvatar.itemId);
        }

        super.buttonClicked(buttonId);
    }

    /** The field for editing the user's display name. */
    protected var _name :TextInput;

    /** The list of our avatars. */
    protected var _avatars :InventoryList;
}
}
