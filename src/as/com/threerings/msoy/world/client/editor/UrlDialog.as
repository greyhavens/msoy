//
// $Id$

package com.threerings.msoy.world.client.editor {

import mx.controls.Button;
import mx.controls.Label;
import mx.controls.TextInput;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;

/**
 * Displays an "Enter the address:" dialog
 */
public class UrlDialog extends FloatingPanel
{
    public function UrlDialog (ctx :MsoyContext, callback :Function)
    {
        super(ctx, Msgs.EDITING.get("t.url_dialog_title"));

        _callback = callback;
        open(true);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        super.buttonClicked(buttonId);
        if (buttonId == OK_BUTTON) {
            _callback(_url.text);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var label :Label = new Label();
        label.text = Msgs.EDITING.get("l.enter_url");
        addChild(label);

        _url = new TextInput();
        _url.text = "http://www.threerings.net/";
        _url.percentWidth = 100;
        _url.maxWidth = 300;
        addChild(_url);

        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    protected var _callback :Function;
    protected var _url :TextInput;
}
}
