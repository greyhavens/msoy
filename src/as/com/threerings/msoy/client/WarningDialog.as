//
// $Id$

package com.threerings.msoy.client {

import flash.events.TimerEvent;
import flash.utils.Timer;

import mx.controls.Label;
import mx.controls.Text;
import mx.containers.VBox;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.ui.FloatingPanel;

public class WarningDialog extends FloatingPanel
{
    public function WarningDialog (ctx :MsoyContext, warning :String)
    {
        super(ctx, Msgs.GENERAL.get("t.warning"));
        open(true);

        _wText.text = warning;
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        var vbox :VBox = new VBox();
        var tlabel :Label = new Label();
        tlabel.text = Msgs.GENERAL.get("l.warning_intro");
        vbox.addChild(tlabel);

        _wText = new Text();
        vbox.addChild(_wText);

        _waitLabel = new Label();
        _waitLabel.text = Msgs.GENERAL.get("m.warning_close_in", "" + CLOSE_TIME);
        _waitLabel.setStyle("fontSize", 10);
        vbox.addChild(_waitLabel);

        addChild(vbox);

        _waitTimer = new Timer(1000, CLOSE_TIME);
        _waitTimer.addEventListener(TimerEvent.TIMER, waitTick);
        _waitTimer.addEventListener(TimerEvent.TIMER_COMPLETE, waitDone);
        _waitTimer.start();
    }

    public function waitTick (event:TimerEvent) :void
    {
        _waitLabel.text =
                Msgs.GENERAL.get("m.warning_close_in", "" + (CLOSE_TIME - _waitTimer.currentCount));
    }

    public function waitDone (event:TimerEvent) :void
    {
        _waitLabel.setVisible(false);
        addButtons(OK_BUTTON);
    }

    override protected function okButtonClicked () :void
    {
        var msvc :MemberService =
            _ctx.getClient().requireService(MemberService) as MemberService;
        msvc.acknowledgeWarning(_ctx.getClient());
    }

    protected var _wText :Text;
    protected var _waitLabel :Label;
    protected var _waitTimer :Timer;

    protected const CLOSE_TIME :int = 30;
}
}
