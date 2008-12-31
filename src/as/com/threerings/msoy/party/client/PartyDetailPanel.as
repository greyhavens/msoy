//
// $Id$

package com.threerings.msoy.party.client {

import mx.containers.HBox;
import mx.containers.VBox;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Roster;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.group.data.all.Group;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldController;

import com.threerings.msoy.party.data.PartyDetail;
import com.threerings.msoy.party.data.PartyPeep;

public class PartyDetailPanel extends FloatingPanel
{
    public function PartyDetailPanel (ctx :WorldContext, detail :PartyDetail)
    {
        super(ctx, detail.info.name);
        showCloseButton = true;
        _detail = detail;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var topBox :HBox = new HBox();
        topBox.addChild(MediaWrapper.createView(Group.logo(_detail.icon)));

        var infoBox :VBox = new VBox();
        infoBox.addChild(FlexUtil.createLabel(_detail.groupName));
        infoBox.addChild(FlexUtil.createLabel(
            Msgs.PARTY.xlate(_detail.info.status), "partyStatus"));
        infoBox.addChild(FlexUtil.createLabel(
            Msgs.PARTY.get("l.recruit_" + _detail.info.recruitment) + "  " +
                _detail.info.population));
        infoBox.addChild(new CommandButton(Msgs.PARTY.get("b.join"),
            WorldController.JOIN_PARTY, _detail.info.id));
        topBox.addChild(infoBox);
        addChild(topBox);

        var roster :Roster = new Roster(_ctx, "",
            PeepRenderer.createFactory(WorldContext(_ctx), _detail.info),
            PartyPeep.createSortByOrder(_detail.info));
        addChild(roster);
        roster.init(_detail.peeps);
    }

    protected var _detail :PartyDetail;
}
}
