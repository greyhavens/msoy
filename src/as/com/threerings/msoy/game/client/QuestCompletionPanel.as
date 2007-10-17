//
// $Id: QuestOfferPanel.as 4826 2007-06-20 20:07:25Z mdb $

package com.threerings.msoy.game.client {

import mx.controls.Text;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.ui.FloatingPanel;

public class QuestCompletionPanel extends FloatingPanel
{
    public function QuestCompletionPanel (ctx :GameContext, outro :String, finish :Function)
    {
        super(ctx.getWorldContext(), Msgs.GAME.get("t.quest_completion"));
        _gctx = ctx;
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
            _finish();
        }
        return super.buttonClicked(buttonId);
    }

    protected var _gctx :GameContext;
    protected var _svc :AVRGameService;

    protected var _finish :Function;
    protected var _outro :String;
}
}
