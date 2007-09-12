//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.BitmapData;
import flash.geom.Matrix;

import mx.core.BitmapAsset;
import mx.controls.Image;
import mx.controls.Text;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.ui.FloatingPanel;

public class SnapshotPanel extends FloatingPanel
{
    public static const IMAGE_HEIGHT :int = 180;
    public static const IMAGE_WIDTH :int = 320;

    public function SnapshotPanel (ctx :WorldContext, ctrl :SnapshotController, view :RoomView)
    {
        super(ctx, Msgs.EDITING.get("t.edit_snapshot"));

        _ctrl = ctrl;
        _bitmap = capture(view);
    }

    /** Captures the current room view into a bitmap. */
    protected function capture (view :RoomView) :BitmapData
    {
        // draw the room, scaling down to the appropriate size
        var newScale :Number = IMAGE_HEIGHT / view.getScrollBounds().height;
        var matrix :Matrix = new Matrix(newScale, 0, 0, newScale);
        var room :BitmapData = new BitmapData(IMAGE_WIDTH, IMAGE_HEIGHT);
        room.draw(view, matrix, null, null, null, true);
        return room;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var label :Text = new Text();
        label.text = Msgs.EDITING.get("l.edit_snapshot_desc");
        label.width = int(IMAGE_WIDTH);
        addChild(label);

        var url :Text = new Text();
        url.text = _ctrl.mediaUrl;
        url.width = int(IMAGE_WIDTH);
        addChild(url);

        _preview = new Image();
        _preview.source = new BitmapAsset(_bitmap);
        addChild(_preview);

        addButtons(FloatingPanel.OK_BUTTON, FloatingPanel.CANCEL_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        super.buttonClicked(buttonId);
        _ctrl.close((buttonId == OK_BUTTON) ? _bitmap : null);
    }

    protected var _bitmap :BitmapData;
    protected var _preview :Image;
    protected var _ctrl :SnapshotController;
}
}
