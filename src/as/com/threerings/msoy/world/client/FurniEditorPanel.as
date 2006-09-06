package com.threerings.msoy.world.client {

import mx.controls.TextInput;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.MsoyUI;

public class FurniEditorPanel extends SpriteEditorPanel
{
    public function FurniEditorPanel (ctx :MsoyContext)
    {
        super(ctx);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.action")),
            _action = new TextInput());
    }

    protected var _action :TextInput;
}
}

