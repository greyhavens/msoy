//
// $Id$

package com.threerings.msoy.party.client {

import flash.display.DisplayObject;

import com.whirled.game.client.Thumbnail;

import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.group.data.all.Group;

import com.threerings.msoy.party.data.PartySummary;

/**
 * Adds client-only functionality.
 */
public class PartyGameClientHelper extends PartyGameHelper
{
    override public function populateProperties (o :Object) :void
    {
        super.populateProperties(o);

        o["party_getGroupLogo_v1"] = getGroupLogo_v1;
    }

    protected function getGroupLogo_v1 (partyId :int) :DisplayObject
    {
        var party :PartySummary = getSummary(partyId);
        if (party == null) {
            return new Thumbnail(); // return a blank
        }
        return new Thumbnail(ScalingMediaContainer.createView(Group.logo(party.icon)));
    }
}
}
