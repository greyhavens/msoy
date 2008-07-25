//
// $Id$

package com.threerings.msoy.world.client.snapshot {

import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Matrix;
import flash.geom.Rectangle;

import mx.core.BitmapAsset;
import mx.core.UIComponent;
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
    public static const SCENE_THUMBNAIL_WIDTH :int = 350;
    public static const SCENE_THUMBNAIL_HEIGHT :int = 200;

    public var galleryImage :Snapshot;
    public var sceneThumbnail :Snapshot;

    public function SnapshotPanel (ctx :WorldContext, sceneId :int, view :RoomView)
    {
        super(ctx, Msgs.WORLD.get("t.snap"));

        _sceneId = sceneId;
        _view = view;
        _ctrl = new SnapshotController(ctx, this);
        
        // if the user is permitted to manage the room then enable the taking of canonical snapshots
        _sceneThumbnailPermitted = _view.canManageRoom();
        
        Log.getLog(this).warning("_sceneThumbnailPermitted = "+_sceneThumbnailPermitted);        
        
        sceneThumbnail = new Snapshot(view, SCENE_THUMBNAIL_WIDTH, SCENE_THUMBNAIL_HEIGHT);
        sceneThumbnail.updateSnapshot();
        
        galleryImage = new Snapshot(view, view.width, view.height);
        galleryImage.updateSnapshot();
                
        open();
    }

    /**
     * Return true if the controller should save a scene thumbnail when the panel is closed.
     */
    public function get shouldSaveSceneThumbnail () :Boolean
    {
        return _useAsSceneThumbnail.selected;
    }

    /**
     * Return true if the controller should save a gallery image when the panel is closed.
     */
    public function get shouldSaveGalleryImage () :Boolean
    {
        return _takeGalleryImage.selected;
    }

    protected function takeNewSnapshot (... ignored) :void
    {
        var occs :Boolean = _showOccs.selected;
        if (!occs) {
            _showChat.selected = false;
        }
        _showChat.enabled = occs;
        
        sceneThumbnail.updateSnapshot(occs, _showChat.selected);        
        galleryImage.updateSnapshot(occs, _showChat.selected);        
    }

    /**
     * Should be called if the user changes an option that means we should update other UI elements.
     * Does not take a new snapshot.
     */
    protected function enforceUIInterlocks (... ignored) :void
    {
        getButton(OK_BUTTON).enabled = canSave();
    }

    /**
     * We can save an image if either one or both of the image saving options are selected.
     */
    protected function canSave () :Boolean {
        return shouldSaveGalleryImage || shouldSaveSceneThumbnail;
    }

    protected function addChildIndented (component :UIComponent) :void
    {
        var hbox :HBox = new HBox();
        hbox.addChild(FlexUtil.createSpacer(10));
        hbox.addChild(component);
        addChild(hbox);        
    } 

    override protected function createChildren () :void
    {
        super.createChildren();

        // take gallery image
        _takeGalleryImage = new CommandCheckBox(Msgs.WORLD.get("b.snap_gallery"), enforceUIInterlocks);
        _takeGalleryImage.selected = true;
        addChild(_takeGalleryImage);

        // only add the button to take the canonical snapshot if it's enabled.
        if (_sceneThumbnailPermitted) {
            _useAsSceneThumbnail = new CommandCheckBox(Msgs.WORLD.get("b.snap_scene_thumbnail"), 
                enforceUIInterlocks);
            _useAsSceneThumbnail.selected = false;
            addChild(_useAsSceneThumbnail);
        }

        // show occupants
        _showOccs = new CommandCheckBox(Msgs.WORLD.get("b.snap_occs"), takeNewSnapshot);
        _showOccs.selected = true;
        addChildIndented(_showOccs);
        
        // show chat is indented
        _showChat = new CommandCheckBox(Msgs.WORLD.get("b.snap_overlays"), takeNewSnapshot);
        _showChat.selected = true;
        addChildIndented(_showChat);

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
        _preview.source = new BitmapAsset(sceneThumbnail.bitmap);
        addChild(_preview);

//        if (!_success) {
//            var msg :Text = new Text();
//            msg.text = Msgs.WORLD.get("m.snapshot_some_failed");
//            msg.width = int(IMAGE_WIDTH);
//            addChild(msg);
//        }

        addButtons(OK_BUTTON, CANCEL_BUTTON);
        enforceUIInterlocks();        
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        super.buttonClicked(buttonId);
        _ctrl.close(buttonId == OK_BUTTON, this, _sceneId);
    }

    protected var _sceneThumbnailPermitted :Boolean;

    protected var _sceneId :int;
    protected var _preview :Image;
    protected var _view :RoomView;
    protected var _ctrl :SnapshotController;

    // UI Elements
    protected var _showOccs :CommandCheckBox;
    protected var _showChat :CommandCheckBox;
    protected var _useAsSceneThumbnail :CommandCheckBox;
    protected var _takeGalleryImage :CommandCheckBox;

//    /** Were we successful in snapshotting every single scene element? */
//    protected var _success :Boolean;
}
}
