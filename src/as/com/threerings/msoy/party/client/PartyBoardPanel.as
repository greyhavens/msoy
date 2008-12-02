//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.ResultAdapter;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCodes;

public class PartyBoardPanel extends FloatingPanel
{
    public function PartyBoardPanel (ctx :MsoyContext, pbsvc :PartyBoardService)
    {
        super(ctx, Msgs.PARTY.get("t.board"));
        showCloseButton = true;

        _pbsvc = pbsvc;

        getPartyBoard();
    }

    protected function getPartyBoard (query :String = null) :void
    {
        _pbsvc.getPartyBoard(_ctx.getClient(), query, new ResultAdapter(
            function (fail :String) :void {
                _ctx.displayFeedback(MsoyCodes.PARTY_MSGS, fail);
            }, gotPartyBoard));
    }

    /**
     * Called with the result of a getPartyBoard request.
     */
    protected function gotPartyBoard (result :Object) :void
    {
        // TODO
    }

    protected var _pbsvc :PartyBoardService;
}
}
