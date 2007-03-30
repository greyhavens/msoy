//
// $Id$

package com.threerings.msoy.client {

import mx.controls.Label;
import mx.controls.Text;

import mx.containers.VBox;

import com.threerings.util.MessageBundle;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyUI;

/**
 * Displays a simple "About Whirled" dialog.
 */
public class AboutDialog extends FloatingPanel
{
    public function AboutDialog (ctx :WorldContext)
    {
        super(ctx, Msgs.GENERAL.get("t.about"));
        open(false);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var textArea :Text = new Text();
        textArea.width = 300;
        textArea.htmlText = Msgs.GENERAL.get("m.about");
        addChild(textArea);

        // for now, let's add in our secret admin/support options to this panel.
        if (_ctx.getMemberObject().tokens.isSupport()) {
            var vbox :VBox = new VBox();
            vbox.setStyle("borderStyle", "inset");
            vbox.setStyle("paddingRight", 20);
            vbox.setStyle("paddingLeft", 20);
            vbox.setStyle("paddingBottom", 10);
            var lbl :Label = new Label();
            lbl.text = "Secret support buttons:";
            vbox.addChild(lbl);

            var pets :CommandButton = new CommandButton(MsoyController.SHOW_PETS);
            pets.label = "show pets dialog...";
            vbox.addChild(pets);

            addChild(vbox);
        }

        addButtons(OK_BUTTON);
    }
}
}
