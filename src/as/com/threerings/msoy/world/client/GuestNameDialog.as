//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.Event;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Text;
import mx.controls.TextInput;
import mx.events.FlexEvent;

import com.threerings.util.Log;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.FloatingPanel;

public class GuestNameDialog extends FloatingPanel
{
    public static const log :Log = Log.getLog(GuestNameDialog);

    public function GuestNameDialog (ctx :WorldContext)
    {
        super(ctx, Msgs.WORLD.get("t.guest_welcome"));
        _wctx = ctx;

        showCloseButton = true;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var contents :VBox = new VBox();

        var intro :Text = new Text();
        intro.text = Msgs.WORLD.get("p.guest_intro");
        intro.setStyle("fontSize", 12);
        intro.percentWidth = 100;
        contents.addChild(intro);

        var id :int = _wctx.getMemberObject().getMemberId();
        _name = new TextInput();
        _name.percentWidth = 100;
        _name.text = Msgs.WORLD.get("m.guest_name", String(id % 1000));

        _name.addEventListener(FlexEvent.ENTER, function (evt :FlexEvent) :void {
            handleSetName();
        });

        _name.addEventListener(Event.CHANGE, function (evt :Event) :void {
            setName.enabled = _name.text.length > 0;
        });

        contents.addChild(_name);

        var setName :CommandButton = new CommandButton(
            Msgs.WORLD.get("b.guest_set_name"), handleSetName);
        setName.styleName = "orangeButton";

        var saveAccount :CommandButton = new CommandButton(
            Msgs.WORLD.get("b.guest_save_account"), handleSaveAccount);
        saveAccount.styleName = "orangeButton";

        var buttons :HBox = new HBox();
        buttons.addChild(setName);
        buttons.addChild(saveAccount);
        buttons.width = WIDTH;

        contents.addChild(buttons);

        addChild(contents);

        _name.setSelection(0, _name.text.length);
        _name.setFocus();
    }

    protected function handleSetName () :void
    {
        if (_name.text.length > 0) {
            _wctx.getMemberDirector().setDisplayName(_name.text);
            close();
        }
    }

    protected function handleSaveAccount () :void
    {
    }

    protected var _wctx :WorldContext;
    protected var _name :TextInput;

    protected static const WIDTH :int = 250;
}
}
