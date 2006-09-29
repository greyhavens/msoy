//
// $Id$

package com.threerings.msoy.client {

import mx.controls.TextInput;

import com.threerings.util.StringUtil;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.MsoyUI;

public class PrefsDialog extends FloatingPanel
{
    public function PrefsDialog (ctx :MsoyContext)
    {
        super(ctx, ctx.xlate("general", "t.prefs"));
        open(true);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var grid :Grid = new Grid();
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("general", "l.display_name")),
            _name = new TextInput());
        _name.text = _ctx.getClientObject().memberName.toString();

        addChild(grid);

        addButtons(OK_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        // save any changed info
        var newName :String = StringUtil.trim(_name.text);
        if (_ctx.getClientObject().memberName.toString() !== newName) {
            _ctx.getMemberDirector().setDisplayName(newName);
        }

        super.buttonClicked(buttonId);
    }

    /** The field for editing the user's display name. */
    protected var _name :TextInput;
}
}
