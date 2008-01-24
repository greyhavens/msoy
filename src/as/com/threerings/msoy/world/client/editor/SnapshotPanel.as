//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Matrix;

import mx.core.BitmapAsset;
import mx.controls.Image;
import mx.controls.Text;

import com.threerings.util.Log;

import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.OccupantSprite;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.WorldContext;

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

    /**
     * Captures the current room view into a bitmap.
     */
    protected function capture (
        view :RoomView, includeOccupants :Boolean = true, includeOverlays :Boolean = true)
        :BitmapData
    {
        // draw the room, scaling down to the appropriate size
        var newScale :Number = IMAGE_HEIGHT / view.getScrollBounds().height;
        var room :BitmapData = new BitmapData(IMAGE_WIDTH, IMAGE_HEIGHT);
        var allSuccess :Boolean = true;

        for (var ii :int = 0; ii < view.numChildren; ii++) {
            var child :DisplayObject = view.getChildAt(ii);

            var matrix :Matrix = child.transform.matrix; // makes a clone...
            matrix.scale(newScale, newScale);

            if (child is MsoySprite) {
                if (!includeOccupants && (child is OccupantSprite)) {
                    continue; // skip occupants
                }

                var success :Boolean = MsoySprite(child).snapshot(room, matrix);
                allSuccess &&= success;

            } else {
                try {
                    room.draw(child, matrix, null, null, null, true);
                    //trace("== Snapshot: raw sprite");

                } catch (err :SecurityError) {
                    // not a critical error
                    Log.getLog(this).info("Unable to snapshot Room element: " + err);
                    allSuccess = false;
                }
            }
        }

        if (includeOverlays) {
            var d :DisplayObject = view;
            while (!(d is LayeredContainer) && d.parent != null) {
                d = d.parent;
            }
            if (d is LayeredContainer) {
                (d as LayeredContainer).snapshotOverlays(room, matrix);
            }
        }

        _success = allSuccess;
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

        if (!_success) {
            var msg :Text = new Text();
            msg.text = Msgs.EDITING.get("m.snapshot_some_failed");
            msg.width = int(IMAGE_WIDTH);
            addChild(msg);
        }

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

    /** Were we successful in snapshotting every single scene element? */
    protected var _success :Boolean;
}
}
