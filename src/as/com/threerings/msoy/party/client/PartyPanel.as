//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.ResultAdapter;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.Roster;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.party.data.PartyObject;

public class PartyPanel extends FloatingPanel
{
    public function PartyPanel (ctx :MsoyContext, partyObj :PartyObject)
    {
        super(ctx, Msgs.PARTY.get("t.party"));
        showCloseButton = true;

        _partyObj = partyObj;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _roster = new Roster(_ctx, PartyObject.PEEPS, MateRenderer);
        _roster.init(_partyObj.peeps.toArray());
        _partyObj.addListener(_roster);

        addChild(_roster);
    }

    override public function close () :void
    {
        _partyObj.removeListener(_roster);

        super.close();
    }

    protected var _partyObj :PartyObject;
    protected var _roster :Roster;
}
}

internal class MateRenderer { } // TODO: Placeholder
