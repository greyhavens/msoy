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
    public static const CANONICAL_WIDTH :int = 350; // old: 320;
    public static const CANONICAL_HEIGHT :int = 200; // old: 180;

    public var fullRoom :Snapshot;
    public var canonical :Snapshot

    public function SnapshotPanel (ctx :WorldContext, sceneId :int, view :RoomView)
    {
        super(ctx, Msgs.WORLD.get("t.snap"));

        _sceneId = sceneId;
        _view = view;
        _ctrl = new SnapshotController(ctx, this);
        
        // if the user is permitted to manage the room then enable the taking of canonical snapshots
        _canonicalEnabled = _view.canManageRoom();
        
        Log.getLog(this).warning("_canonicalEnabled = "+_canonicalEnabled);        
        
        if (_canonicalEnabled) {
            canonical = new Snapshot(view, CANONICAL_WIDTH, CANONICAL_HEIGHT);
            canonical.updateSnapshot();
        }
        
        fullRoom = new Snapshot(view, view.width, view.height);
        fullRoom.updateSnapshot();
        
        open();
    }

    protected function takeNewSnapshot (... ignored) :void
    {
        var occs :Boolean = _showOccs.selected;
        if (!occs) {
            _showChat.selected = false;
        }
        _showChat.enabled = occs;
        
        if (_canonicalEnabled) {
            canonical.updateSnapshot(occs, _showChat.selected);
        }
        
        fullRoom.updateSnapshot(occs, _showChat.selected);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _showOccs = new CommandCheckBox(Msgs.WORLD.get("b.snap_occs"), takeNewSnapshot);
        _showChat = new CommandCheckBox(Msgs.WORLD.get("b.snap_overlays"), takeNewSnapshot);
        _showOccs.selected = true;
        _showChat.selected = true;

        addChild(_showOccs);

        var hbox :HBox = new HBox();
        hbox.addChild(FlexUtil.createSpacer(10));
        hbox.addChild(_showChat);
        addChild(hbox);

        // only add the button to take the canonical snapshot if it's enabled.
        if (_canonicalEnabled) {
            _useAsCanonical = new CommandCheckBox(Msgs.WORLD.get("b.snap_canonical"), 
                takeNewSnapshot);
            _useAsCanonical.selected = false;
            addChild(_useAsCanonical);
        }

        addChild(new CommandButton(Msgs.WORLD.get("b.snap_update"), takeNewSnapshot));

//        var label :Text = new Text();
//        label.text = Msgs.WORLD.get("l.edit_canonical_desc");
//        label.width = int(IMAGE_WIDTH);
//        label.selectable = false;
//        addChild(label);
//
//        var url :Text = new Text();
//        url.text = _ctrl.mediaUrl;
//        url.width = int(IMAGE_WIDTH);
//        addChild(url);

        _preview = new Image();
        _preview.source = new BitmapAsset(canonical.bitmap);
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
        _ctrl.close(buttonId == OK_BUTTON, this, _sceneId);
    }

    protected var _sceneId :int;
    protected var _preview :Image;
    protected var _view :RoomView;
    protected var _ctrl :SnapshotController;
    protected var _canonicalEnabled :Boolean;

    protected var _showOccs :CommandCheckBox;
    protected var _showChat :CommandCheckBox;
    protected var _useAsCanonical :CommandCheckBox;

//    /** Were we successful in snapshotting every single scene element? */
//    protected var _success :Boolean;
}
}
