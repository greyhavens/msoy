//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObjectContainer;

import flash.events.DataEvent;
import flash.events.MouseEvent;

import mx.binding.utils.BindingUtils;

import mx.core.Container;

import mx.controls.Button;
import mx.controls.CheckBox;
import mx.controls.ComboBox;
import mx.controls.Label;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;
import mx.controls.TextInput;
import mx.controls.Tree;

import mx.containers.TabNavigator;
import mx.containers.VBox;

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

        var tabs :TabNavigator = new TabNavigator();
        tabs.resizeToContent = true;
        tabs.addChild(createPrimaryTab());
        tabs.addChild(createChatTab());

        addChild(tabs);

        addButtons(OK_BUTTON);
    }

    protected function createPrimaryTab () :Container
    {
        var tainer :VBox = new VBox();
        tainer.label = Msgs.PREFS.get("t.general");

        var memberObj :MemberObject = _ctx.getClientObject();

        var grid :Grid = new Grid();
        grid.addRow(MsoyUI.createLabel(Msgs.GENERAL.get("l.display_name")),
                    _name = new TextInput());
        _name.text = memberObj.memberName.toString();

        grid.addRow(MsoyUI.createLabel(Msgs.GENERAL.get("l.log_to_chat")),
                    _logToChat = new CheckBox());
        _logToChat.selected = Prefs.getLogToChat();
        BindingUtils.bindSetter(Prefs.setLogToChat, _logToChat, "selected");

        tainer.addChild(grid);

        _avatars.percentWidth = 100;
        _avatars.tree.dragEnabled = false;

        tainer.addChild(_avatars);

        var btn :CommandButton = new CommandButton(MsoyController.PURCHASE_ROOM);
        btn.label = Msgs.GENERAL.get("b.purchase_room");
        tainer.addChild(btn);

        return tainer;
    }

    protected function createChatTab () :Container
    {
        var tainer :VBox = new VBox();
        tainer.label = Msgs.PREFS.get("t.chat");

        var ii :int;
        var grid :Grid = new Grid();

        var decay :ComboBox = new ComboBox();
        var choices :Array = [];
        for (ii = 0; ii < 3; ii++) {
            choices.push(Msgs.PREFS.get("l.chat_decay" + ii));
        }
        decay.dataProvider = choices;
        decay.selectedIndex = Prefs.getChatDecay();
        BindingUtils.bindSetter(Prefs.setChatDecay, decay, "selectedIndex");
        grid.addRow(Msgs.PREFS.get("l.chat_decay"), decay);

        grid.addRow(Msgs.PREFS.get("l.chat_filter"), [2, 1]);
        var filterGroup :RadioButtonGroup = new RadioButtonGroup();
        var filterChoice :int = Prefs.getChatFilterLevel();
        for (ii= 0; ii < 4; ii++) {
            var but :RadioButton = new RadioButton();
            but.label = Msgs.PREFS.get("l.chat_filter" + ii);
            but.selected = (ii == filterChoice);
            but.value = ii;
            but.group = filterGroup;
            grid.addRow(but, [2, 1]);
        }
        BindingUtils.bindSetter(Prefs.setChatFilterLevel, filterGroup, "selectedValue");

        var lbl :Label = new Label();
        lbl.text = Msgs.PREFS.get("m.chat_filter_note");
        lbl.setStyle("fontSize", 8);
        grid.addRow(lbl, [2, 1]);

        tainer.addChild(grid);
        return tainer;
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
