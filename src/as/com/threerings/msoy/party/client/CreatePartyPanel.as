//
// $Id$

package com.threerings.msoy.party.client {

import mx.containers.Grid;

import mx.controls.ComboBox;
import mx.controls.TextInput;

import com.threerings.util.Integer;
import com.threerings.util.StringUtil;

import com.threerings.flex.FlexUtil;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.group.data.all.GroupMembership;

import com.threerings.msoy.party.data.PartyCodes;

/**
 * A dialog used to configure a new party for creation.
 */
public class CreatePartyPanel extends FloatingPanel
{
    public function CreatePartyPanel (ctx :MsoyContext)
    {
        super(ctx, Msgs.PARTY.get("t.create"));
        setButtonWidth(0);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // see which groups are valid
        var us :MemberObject = (_ctx.getClient().getClientObject() as MemberObject);
        if (us.groups.size() == 0) {
            addChild(FlexUtil.createLabel(Msgs.PARTY.get("e.party_need_group")));
            addButtons(CANCEL_BUTTON);
            return;
        }

        // sort all the groups

        _name = new TextInput();
        _name.maxChars = PartyCodes.MAX_NAME_LENGTH;
        _name.text = StringUtil.truncate(
            Msgs.PARTY.get("m.default_name", us.memberName.toString()), PartyCodes.MAX_NAME_LENGTH);

        _group = new ComboBox();
        _group.dataProvider = us.groups.toArray().sort(
            function (one :GroupMembership, two :GroupMembership) :int {
                // sort groups by your rank, then by name
                var cmp :int = Integer.compare(one.rank, two.rank);
                if (cmp == 0) {
                    cmp = MemberName.BY_DISPLAY_NAME(one.group, two.group);
                }
                return cmp;
            }).map(function (item :GroupMembership, ... rest) :Object {
                return { label: item.group.toString(), data: item.group.getGroupId() };
            });

        var grid :Grid = new Grid();
        GridUtil.addRow(grid, Msgs.PARTY.get("l.name"), _name);
        GridUtil.addRow(grid, Msgs.PARTY.get("l.group"), _group);
        addChild(grid);

        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        switch (buttonId) {
        case OK_BUTTON: return Msgs.PARTY.get("b.create");
        default: return super.getButtonLabel(buttonId);
        }
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON) {
            _ctx.getPartyDirector().createParty(_name.text, int(_group.selectedItem.data));
        }

        super.buttonClicked(buttonId);
    }

    protected var _name :TextInput;

    protected var _group :ComboBox;
}
}
