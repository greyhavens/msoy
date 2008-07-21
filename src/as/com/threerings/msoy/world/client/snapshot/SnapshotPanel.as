//
// $Id$

package com.threerings.msoy.world.client.snapshot {

import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Matrix;
import flash.geom.Rectangle;

import mx.core.BitmapAsset;
import mx.controls.Image;
import mx.controls.Text;

import mx.containers.HBox;

import com.threerings.util.Log;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.OccupantSprite;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.WorldContext;

public class SnapshotPanel extends FloatingPanel
{
    public static const IMAGE_WIDTH :int = 380; // old: 320;
    public static const IMAGE_HEIGHT :int = 167; // old: 180;

    public function SnapshotPanel (ctx :WorldContext, sceneId :int, view :RoomView)
    {
        super(ctx, Msgs.WORLD.get("t.snap"));

        _ctrl = new SnapshotController(ctx, this);
        _view = view;
        _bitmap = new BitmapData(IMAGE_WIDTH, IMAGE_HEIGHT);
        updateSnapshot();
        open();
    }

    /**
     * Update the snapshot.
     */
    public function updateSnapshot (
        includeOccupants :Boolean = true, includeOverlays :Boolean = true) :Boolean
    {
        // first let's fill the bitmap with black or something
        _bitmap.fillRect(new Rectangle(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT), 0x000000);

        // draw the room, scaling down to the appropriate size
        var newScale :Number = IMAGE_HEIGHT / _view.getScrollBounds().height;
        var allSuccess :Boolean = true;

        for (var ii :int = 0; ii < _view.numChildren; ii++) {
            var child :DisplayObject = _view.getChildAt(ii);

            var matrix :Matrix = child.transform.matrix; // makes a clone...
            matrix.scale(newScale, newScale); // scales the matrix taken from that child object

            if (child is MsoySprite) {
                if (!includeOccupants && (child is OccupantSprite)) {
                    continue; // skip occupants
                }

                var success :Boolean = MsoySprite(child).snapshot(_bitmap, matrix);
                allSuccess &&= success;

            } else {
                try {
                    _bitmap.draw(child, matrix, null, null, null, true);
                    //trace("== Snapshot: raw sprite");

                } catch (err :SecurityError) {
                    // not a critical error
                    Log.getLog(this).info("Unable to snapshot Room element: " + err);
                    allSuccess = false;
                }
            }
        }

        if (includeOverlays) {
            var d :DisplayObject = _view;
            
            // search up through the containment hierarchy until you find the LayeredContainer
            // or the end of the hierarchy
            while (!(d is LayeredContainer) && d.parent != null) {
                d = d.parent;
            }
            if (d is LayeredContainer) {
                // where does this matrix come from?  The last child in the original transformation?
                (d as LayeredContainer).snapshotOverlays(_bitmap, newScale);
            }
        }

        return allSuccess;
    }

    protected function takeNewSnapshot (... ignored) :void
    {
        var occs :Boolean = _showOccs.selected;
        if (!occs) {
            _showChat.selected = false;
        }
        _showChat.enabled = occs;
        updateSnapshot(occs, _showChat.selected);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _useAsCanonical = new CommandCheckBox(Msgs.WORLD.get("b.snap_canonical"));
        _showOccs = new CommandCheckBox(Msgs.WORLD.get("b.snap_occs"), takeNewSnapshot);
        _showChat = new CommandCheckBox(Msgs.WORLD.get("b.snap_overlays"), takeNewSnapshot);
        _showOccs.selected = true;
        _showChat.selected = true;
        _useAsCanonical.selected = false;

        addChild(_useAsCanonical);
        addChild(_showOccs);

        var hbox :HBox = new HBox();
        hbox.addChild(FlexUtil.createSpacer(10));
        hbox.addChild(_showChat);
        addChild(hbox);

        addChild(new CommandButton(Msgs.WORLD.get("b.snap_update"), takeNewSnapshot));

//        var label :Text = new Text();
//        label.text = Msgs.WORLD.get("l.edit_snapshot_desc");
//        label.width = int(IMAGE_WIDTH);
//        label.selectable = false;
//        addChild(label);
//
//        var url :Text = new Text();
//        url.text = _ctrl.mediaUrl;
//        url.width = int(IMAGE_WIDTH);
//        addChild(url);

        _preview = new Image();
        _preview.source = new BitmapAsset(_bitmap);
        addChild(_preview);

//        if (!_success) {
//            var msg :Text = new Text();
//            msg.text = Msgs.WORLD.get("m.snapshot_some_failed");
//            msg.width = int(IMAGE_WIDTH);
//            addChild(msg);
//        }

        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        super.buttonClicked(buttonId);
        _ctrl.close((buttonId == OK_BUTTON) ? _bitmap : null);
    }

    protected var _bitmap :BitmapData;
    protected var _preview :Image;
    protected var _view :RoomView;
    protected var _ctrl :SnapshotController;

    protected var _showOccs :CommandCheckBox;
    protected var _showChat :CommandCheckBox;
    protected var _useAsCanonical :CommandCheckBox;

//    /** Were we successful in snapshotting every single scene element? */
//    protected var _success :Boolean;
}
}
