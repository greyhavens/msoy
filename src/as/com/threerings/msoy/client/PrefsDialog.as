//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObjectContainer;

import flash.events.DataEvent;
import flash.events.MouseEvent;

import mx.controls.Button;
import mx.controls.CheckBox;
import mx.controls.TextInput;
import mx.controls.Tree;

import com.threerings.util.StringUtil;

import com.threerings.mx.controls.CommandButton;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.client.InventoryList;
import com.threerings.msoy.item.client.InventoryPicker;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Avatar;

public class PrefsDialog extends FloatingPanel
    implements AttributeChangeListener
{
    public function PrefsDialog (ctx :MsoyContext)
    {
        super(ctx, Msgs.GENERAL.get("t.prefs"));
        _avatars = new InventoryPicker(_ctx, Item.AVATAR, true);
        open(true);
    }

    // from AttributeChangeListener
    public function attributeChanged (evt :AttributeChangedEvent) :void
    {
        if (MemberObject.LOADED_INVENTORY == evt.getName() &&
                _memberObj.isInventoryLoaded(Item.AVATAR)) {
            unwatchPlayer();
            callLater(selectCurrentAvatar);
        }
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p != null) {
            var memberObject :MemberObject = _ctx.getClientObject();
            if (memberObject.isInventoryLoaded(Item.AVATAR)) {
                // call later, twice, so that the picker has had time to set up the avatars. Kinda
                // hacky, but what to do?
                callLater(callLater, [selectCurrentAvatar]);

            } else {
                _memberObj = memberObject;
                _memberObj.addListener(this);
            }

        } else {
            unwatchPlayer();
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var memberObj :MemberObject = _ctx.getClientObject();

        var grid :Grid = new Grid();
        grid.addRow(MsoyUI.createLabel(Msgs.GENERAL.get("l.display_name")),
                    _name = new TextInput());
        _name.text = memberObj.memberName.toString();

        grid.addRow(MsoyUI.createLabel(Msgs.GENERAL.get("l.log_to_chat")),
                    _logToChat = new CheckBox());
        _logToChat.selected = Prefs.getLogToChat();
        _logToChat.addEventListener(MouseEvent.CLICK, function (evt :MouseEvent) :void {
            Prefs.setLogToChat(_logToChat.selected);
        });

        addChild(grid);

        _avatars.percentWidth = 100;
        _avatars.tree.dragEnabled = false;

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
        var newAvatar :Avatar = (_avatars.getSelectedItem() as Avatar);
        if (newAvatar != null && !newAvatar.equals(memberObj.avatar)) {
            _ctx.getMemberDirector().setAvatar(newAvatar.itemId);
        }

        super.buttonClicked(buttonId);
    }

    protected function unwatchPlayer () :void
    {
        if (_memberObj != null) {
            _memberObj.removeListener(this);
            _memberObj = null;
        }
    }

    protected function selectCurrentAvatar () :void
    {
        var memberObj :MemberObject = _ctx.getClientObject();
        var memberAv :Avatar = memberObj.avatar;
        if (memberAv == null) {
            return;
        }

        for each (var av :Avatar in memberObj.getItems(Item.AVATAR)) {
            if (av.itemId == memberAv.itemId) {
                _avatars.setSelectedItem(av);
                break;
            }
        }
    }

    /** The field for editing the user's display name. */
    protected var _name :TextInput;

    /** Whether or not we route log messages to chat for easy debugging. */
    protected var _logToChat :CheckBox;

    /** The list of our avatars. */
    protected var _avatars :InventoryPicker;

    /** The member object of our player, while we're listening for avatars to be loaded. */
    protected var _memberObj :MemberObject;
}
}
