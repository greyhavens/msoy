//
// $Id$

package com.threerings.msoy.party.client {

import mx.containers.Grid;
import mx.controls.CheckBox;
import mx.controls.Text;
import mx.controls.TextInput;
import mx.core.ClassFactory;

import com.threerings.util.ComparisonChain;
import com.threerings.util.StringUtil;

import com.threerings.orth.data.OrthName;

import com.threerings.flex.CommandComboBox;
import com.threerings.flex.FlexUtil;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.money.client.BuyPanel;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.world.client.WorldContext;

/**
 * A dialog used to configure a new party for creation.
 */
public class CreatePartyPanel extends FloatingPanel
{
    public function CreatePartyPanel (ctx :WorldContext, quote :PriceQuote = null)
    {
        super(ctx, Msgs.PARTY.get("t.create"));
        setButtonWidth(0);

        _buyPanel = new BuyPanel(_ctx, createParty);

        // if we have no price quote, fire off a request for one
        if (quote != null) {
            _buyPanel.setPriceQuote(quote);

        } else {
            WorldContext(_ctx).getPartyDirector().getCreateCost(_buyPanel.setPriceQuote);
        }
    }

    /**
     * Can be called after we're open to re-init the values.
     */
    public function init (name :String, groupId :int, inviteAllFriends :Boolean) :void
    {
        _name.text = name;
        _group.selectedData = groupId;
        _inviteAll.selected = inviteAllFriends;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // see which groups are valid
        var us :MemberObject = _ctx.getMemberObject();
        if (us.groups == null || us.groups.size() == 0) {
            addChild(FlexUtil.createLabel(Msgs.PARTY.get("e.party_need_group")));
            addButtons(CANCEL_BUTTON);
            return;
        }

        // sort all the groups

        _name = new TextInput();
        _name.maxChars = PartyCodes.MAX_NAME_LENGTH;
        _name.text = StringUtil.truncate(
            Msgs.PARTY.get("m.default_name", us.memberName.toString()), PartyCodes.MAX_NAME_LENGTH);

        _group = new CommandComboBox();
        _group.itemRenderer = new ClassFactory(GroupLabel);
        _group.dataProvider = us.groups.toArray().sort(
            function (one :GroupMembership, two :GroupMembership) :int {
                // sort groups by your rank, then by name
                return ComparisonChain.start()
                    .compareComparables(two.rank, one.rank) // higher rank sorts first
                    .compare(one.group, two.group, OrthName.BY_DISPLAY_NAME)
                    .result();
            }).map(function (item :GroupMembership, ... rest) :Object {
                return { label: item.group.toString(), data: item.group.getGroupId(),
                    rank: item.rank };
            });
        // attempt to select the group they used last time
        _group.selectedData = Prefs.getPartyGroup();
        _group.selectedIndex = Math.max(0, _group.selectedIndex); // make sure something's selected

        _inviteAll = new CheckBox();
        _inviteAll.selected = true;

        var grid :Grid = new Grid();
        GridUtil.addRow(grid, Msgs.PARTY.get("l.name"), _name);
        GridUtil.addRow(grid, Msgs.PARTY.get("l.group"), _group);
        GridUtil.addRow(grid, Msgs.PARTY.get("l.invite_friends"), _inviteAll);
        addChild(grid);

        addChild(_buyPanel);
        addButtons(CANCEL_BUTTON);
    }

    protected function createParty (currency :Currency, authedCost :int) :void
    {
        WorldContext(_ctx).getPartyDirector().createParty(
            currency, authedCost, _name.text, int(_group.selectedData), _inviteAll.selected);
        close();
    }

    protected var _buyPanel :BuyPanel;

    protected var _name :TextInput;

    protected var _group :CommandComboBox;

    protected var _inviteAll :CheckBox;
}
}

import mx.controls.Label;

import com.threerings.msoy.group.data.all.GroupMembership_Rank;

/**
 * Bolds groups in which we're a manager.
 */
class GroupLabel extends Label
{
    override public function set data (value :Object) :void
    {
        super.data = value;
        setStyle("fontWeight", (value.rank == GroupMembership_Rank.MANAGER) ? "bold" : "normal");
    }
}
