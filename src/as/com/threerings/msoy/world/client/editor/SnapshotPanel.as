//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.BitmapData;

import mx.core.BitmapAsset;
import mx.controls.Image;
import mx.controls.Text;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.ui.FloatingPanel;

public class SnapshotPanel extends FloatingPanel
{
    public function SnapshotPanel (ctx :WorldContext, bitmap :BitmapData, callback :Function)
    {
        super(ctx, Msgs.EDITING.get("t.edit_snapshot"));

        _callback = callback;
        _bitmap = bitmap;
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        
        var label :Text = new Text();
        label.text = Msgs.EDITING.get("l.edit_snapshot_desc");
        label.width = int(SnapshotController.IMAGE_WIDTH);
        addChild(label);

        _preview = new Image();
        _preview.source = new BitmapAsset(_bitmap);
        addChild(_preview);

        addButtons(FloatingPanel.OK_BUTTON, FloatingPanel.CANCEL_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        super.buttonClicked(buttonId);
        _callback(buttonId == OK_BUTTON);
    }
    
    protected var _bitmap :BitmapData;
    protected var _preview :Image;
    protected var _callback :Function;
}
}
