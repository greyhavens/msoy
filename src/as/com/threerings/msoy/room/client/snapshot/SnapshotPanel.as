//
// $Id$

package com.threerings.msoy.room.client.snapshot {

import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Matrix;
import flash.geom.Rectangle;

import mx.core.BitmapAsset;
import mx.core.Container;
import mx.core.UIComponent;
import mx.controls.Image;
import mx.controls.Text;
import mx.controls.ProgressBar;
import mx.controls.Label;

import mx.containers.HBox;
import mx.containers.VBox;

import com.threerings.util.Log;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.MsoySprite;
import com.threerings.msoy.room.client.OccupantSprite;
import com.threerings.msoy.room.client.RoomView;

public class SnapshotPanel extends FloatingPanel
{
    public static const SCENE_THUMBNAIL_WIDTH :int = 350;
    public static const SCENE_THUMBNAIL_HEIGHT :int = 200;

    public var galleryImage :Snapshot;
    public var sceneThumbnail :Snapshot;

    public function SnapshotPanel (ctx :WorldContext)
    {
        super(ctx, Msgs.WORLD.get("t.snap"));

        _view = ctx.getTopPanel().getPlaceView() as RoomView;
        _ctrl = new SnapshotController(ctx, _view, this);

        // if the user is permitted to manage the room then enable the taking of canonical snapshots
        _sceneThumbnailPermitted = _view.getRoomController().canManageRoom();

        Log.getLog(this).debug("_sceneThumbnailPermitted = "+_sceneThumbnailPermitted);        

        sceneThumbnail = new Snapshot(_view, SCENE_THUMBNAIL_WIDTH, SCENE_THUMBNAIL_HEIGHT);
        sceneThumbnail.updateSnapshot();

        // TODO: we want the room bounds, not the room *view* bounds....
        galleryImage = new Snapshot(_view, _view.width, _view.height);
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
    protected function canSave () :Boolean
    {
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
        createSnapshotControls(this);                
    }
    
    protected function showProgressBar () :void
    {
        removeAllChildren();
        super.createChildren();
        createProgressControls(this);
    }
    
    protected function createProgressControls (container :Container) :void
    {
        var bar :ProgressBar = new ProgressBar();
        bar.percentWidth = 100;
        bar.indeterminate = true;
        bar.label = Msgs.WORLD.get("b.snap_progress");
        container.addChild(bar);
        _progressLabel = new Label();
        _progressLabel.text = Msgs.WORLD.get("b.snap_upload_starting");
        container.addChild(_progressLabel);   
        _cancelUploadButton = new CommandButton(Msgs.WORLD.get("b.snap_cancel"), cancelUpload);     
        container.addChild(_cancelUploadButton);        
    }

    /**
     * Handling the user request that the upload be cancelled.
     */
    protected function cancelUpload () :void 
    {
        // cancel any encoding processes that may be running.
        galleryImage.cancelEncoding();
        sceneThumbnail.cancelEncoding();
        
        // close the panel
        close();
    }

    protected function createSnapshotControls (container :Container) :void
    {
        // take gallery image
        _takeGalleryImage = new CommandCheckBox(Msgs.WORLD.get("b.snap_gallery"), 
            enforceUIInterlocks);
        _takeGalleryImage.selected = true;
        container.addChild(_takeGalleryImage);

        // only add the button to take the canonical snapshot if it's enabled.
        if (_sceneThumbnailPermitted) {
            _useAsSceneThumbnail = new CommandCheckBox(Msgs.WORLD.get("b.snap_scene_thumbnail"), 
                enforceUIInterlocks);
            _useAsSceneThumbnail.selected = false;
            container.addChild(_useAsSceneThumbnail);
        }

        // show occupants
        _showOccs = new CommandCheckBox(Msgs.WORLD.get("b.snap_occs"), takeNewSnapshot);
        _showOccs.selected = true;
        container.addChild(_showOccs);
        
        // show chat
        _showChat = new CommandCheckBox(Msgs.WORLD.get("b.snap_overlays"), takeNewSnapshot);
        _showChat.selected = true;
        container.addChild(_showChat);

        container.addChild(new CommandButton(Msgs.WORLD.get("b.snap_update"), takeNewSnapshot));

        _preview = new Image();
        _preview.source = new BitmapAsset(sceneThumbnail.bitmap);
        container.addChild(_preview);

        addButtons(OK_BUTTON, CANCEL_BUTTON);
        enforceUIInterlocks();        
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON) {
            upload();
        } else {
            close();            
        }        
    }    

    /**
     * Begin the upload process, much of which happens asynchronously.
     */
    protected function upload () :void
    {
        showProgressBar();
        
        if (this.shouldSaveSceneThumbnail) {
            _progressLabel.text = Msgs.WORLD.get("b.snap_upload_thumb");
            sceneThumbnail.encodeAndUpload(_ctrl.uploadThumbnail, uploadGalleryImage);
        } else {
            uploadGalleryImage();
        }
    }

    /**
     * Second stage of the upload process.
     */
    protected function uploadGalleryImage () :void
    {
        if (this.shouldSaveGalleryImage) {
            _progressLabel.text = Msgs.WORLD.get("b.snap_upload_snap");
            galleryImage.encodeAndUpload(_ctrl.uploadGalleryImage, uploadingDone);            
        } else {
            uploadingDone();
        }
    }
    
    /**
     * Called if uploading failed.
     */
    public function uploadError (message :String) :void
    {
        _progressLabel.text = Msgs.WORLD.get("b.snap_upload_fail");
        _cancelUploadButton.label = Msgs.GENERAL.get("b.ok");
    }
    
    /**
     * Called when uploading is complete.
     */
    protected function uploadingDone () :void
    {
        // done at this point so we can close the panel
        close();        
    }

    protected var _galleryImageDone :Boolean = false;
    protected var _sceneThumbnailDone :Boolean = false;

    protected var _sceneThumbnailPermitted :Boolean;

    protected var _preview :Image;
    protected var _view :RoomView;
    protected var _ctrl :SnapshotController;

    // UI Elements
    protected var _showOccs :CommandCheckBox;
    protected var _showChat :CommandCheckBox;
    protected var _useAsSceneThumbnail :CommandCheckBox;
    protected var _takeGalleryImage :CommandCheckBox;

    protected var _snapPanel :Container;
    protected var _progressPanel :Container;
    protected var _cancelUploadButton :CommandButton;
    protected var _progressLabel :Label;
}
}
