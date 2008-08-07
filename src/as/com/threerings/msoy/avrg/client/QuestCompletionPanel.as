//
// $Id$

package com.threerings.msoy.avrg.client {

import mx.controls.Text;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.world.client.WorldContext;

public class QuestCompletionPanel extends FloatingPanel
{
    public function QuestCompletionPanel (wctx :WorldContext, outro :String, finish :Function)
    {
        super(wctx, Msgs.GAME.get("t.quest_completion"));
        _outro = outro;
        _finish = finish;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var text :Text = new Text();
        text.htmlText = _outro;
        text.width = 200;
        this.addChild(text);

        addButtons(OK_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON) {
            // it's possible for the AVRG to go away while this popup is around
            if (_finish != null) {
                _finish();
            }
        }
        return super.buttonClicked(buttonId);
    }

    protected var _svc :AVRGameService;

    protected var _finish :Function;
    protected var _outro :String;
}
}
