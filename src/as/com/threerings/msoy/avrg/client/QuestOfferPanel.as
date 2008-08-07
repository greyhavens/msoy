//
// $Id$

package com.threerings.msoy.avrg.client {

import mx.controls.Button;
import mx.controls.Text;

import com.threerings.util.CommandEvent;
import com.threerings.util.MessageBundle;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.room.client.WorldContext;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.ui.FloatingPanel;

public class QuestOfferPanel extends FloatingPanel
{
    public static const DECLINE_BUTTON :int = -1;
    public static const ACCEPT_BUTTON :int = -2;

    public function QuestOfferPanel (wctx :WorldContext, intro :String, accept :Function)
    {
        super(wctx, Msgs.GAME.get("t.quest_offer"));
        _intro = intro;
        _accept = accept;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var text :Text = new Text();
        text.htmlText = _intro;
        text.width = 200;
        this.addChild(text);

        addButtons(DECLINE_BUTTON, ACCEPT_BUTTON);
    }

    override protected function createButton (buttonId :int) :Button
    {
        var btn :Button;
        if (buttonId == ACCEPT_BUTTON) {
            btn = new Button();
            btn.label = Msgs.GAME.get("b.accept_quest");
            return btn;
        }
        if (buttonId == DECLINE_BUTTON) {
            btn = new Button();
            btn.label = Msgs.GAME.get("b.decline_quest");
            return btn;
        }

        return super.createButton(buttonId);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == ACCEPT_BUTTON) {
            // it's possible for the AVRG to go away while this popup is around
            if (_accept != null) {
                _accept();
            }

        } else if (buttonId != DECLINE_BUTTON) {
            return super.buttonClicked(buttonId);
        }
        close();
    }

    protected var _svc :AVRGameService;

    protected var _accept :Function;
    protected var _intro :String;
}
}
