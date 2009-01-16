//
// $Id$

package com.threerings.msoy.world.client {

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.ComboBox;
import mx.controls.Label;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.FloatingPanel;

public class GuestNameDialog extends FloatingPanel
{
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

        var intro :Label = new Label();
        intro.htmlText = Msgs.WORLD.get("p.guest_intro", DeploymentConfig.serverURL);
        intro.setStyle("fontSize", 12);
        intro.percentWidth = 100;
        contents.addChild(intro);

        var names :Array = [];
        for (var ii :int = 0; ii < 7; ++ii) {
            names.push(Msgs.WORLD.get("m.guest_sample" + ii));
        }

        _name = new ComboBox();
        _name.dataProvider = names;
        _name.selectedIndex = int(Math.random() * names.length);
        _name.width = 250;
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
        buttons.percentWidth = 100;

        contents.addChild(buttons);

        addChild(contents);
    }

    protected function handleSetName () :void
    {
    }

    protected function handleSaveAccount () :void
    {
    }

    protected var _wctx :WorldContext;
    protected var _name :ComboBox;
}
}
