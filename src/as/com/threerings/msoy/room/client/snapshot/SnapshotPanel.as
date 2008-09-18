//
// $Id$

package com.threerings.msoy.room.client.snapshot {

import flash.geom.Rectangle;

import mx.core.BitmapAsset;
import mx.core.Container;
import mx.core.UIComponent;
import mx.controls.Image;
import mx.controls.ProgressBar;
import mx.controls.Label;

import mx.containers.HBox;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.data.MsoyScene;

public class SnapshotPanel extends FloatingPanel
{
    public static const SCENE_THUMBNAIL_WIDTH :int = 350;
    public static const SCENE_THUMBNAIL_HEIGHT :int = 200;

    /** This is the maximum bitmap dimension, a flash limitation. */
    public static const MAX_BITMAP_DIM :int = 2880;

    public var galleryImage :Snapshot;
    public var sceneThumbnail :Snapshot;

    public function SnapshotPanel (ctx :WorldContext)
    {
        super(ctx, Msgs.WORLD.get("t.snap"));

        styleName = "sexyWindow";
        setStyle("horizontalAlign", "left");

        _view = ctx.getTopPanel().getPlaceView() as RoomView;
        _ctrl = new SnapshotController(ctx, _view, this);

        // if the user is permitted to manage the room then enable the taking of canonical snapshots
        _sceneThumbnailPermitted = _view.getRoomController().canManageRoom();

        // for the canonical image, we create a new framer that centers the image within the frame, 
        // introducing black bars if necessary.
        const frame :Rectangle = new Rectangle(0, 0, SCENE_THUMBNAIL_WIDTH, SCENE_THUMBNAIL_HEIGHT);
        const framer :Framer = new CanonicalFramer(_view.getScrollBounds(), frame, 
            _view.getScrollOffset());
        
        // take this even if we aren't going to use it, it's used for the preview
        sceneThumbnail = new Snapshot(_view, framer, SCENE_THUMBNAIL_WIDTH, SCENE_THUMBNAIL_HEIGHT);
        sceneThumbnail.updateSnapshot();

        // TODO: we want the room bounds, not the room *view* bounds....
        const scene:MsoyScene = _view.getScene();
        var galWidth :int = scene.getWidth();
        var galHeight :int = scene.getHeight();
        var galFramer :Framer;
        if (galWidth > MAX_BITMAP_DIM || galWidth > MAX_BITMAP_DIM) {
            const galScale :Number = Math.min(MAX_BITMAP_DIM / galWidth,
                MAX_BITMAP_DIM / galHeight);
            galWidth *= galScale;
            galHeight *= galScale;
//            galFramer = new CanonicalFramer(_view.getScrollBounds(),
//                new Rectangle(0, 0, galWidth, galHeight), _view.getScrollOffset());
            // TODO: sort out real offset?
            galFramer = new NoopFramer();

        } else {
            galFramer = new NoopFramer();
        }
        galleryImage = new Snapshot(_view, galFramer, galWidth, galHeight);
        galleryImage.updateSnapshot();
        open();
    }

    /**
     * Return true if the controller should save a scene thumbnail when the panel is closed.
     */
    public function get shouldSaveSceneThumbnail () :Boolean
    {
        return (_useAsSceneThumbnail != null) && _useAsSceneThumbnail.selected;
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
        createSnapshotControls();                
        showCloseButton = true;
    }
    
    protected function showProgressBar () :void
    {
        removeAllChildren();
        super.createChildren();
        createProgressControls();
        showCloseButton = false;
    }
    
    protected function createProgressControls () :void
    {
        var bar :ProgressBar = new ProgressBar();
        bar.percentWidth = 100;
        bar.indeterminate = true;
        bar.label = Msgs.WORLD.get("b.snap_progress");
        addChild(bar);
        _progressLabel = new Label();
        _progressLabel.text = Msgs.WORLD.get("b.snap_upload_starting");
        addChild(_progressLabel);   
        _cancelUploadButton = new CommandButton(Msgs.WORLD.get("b.snap_cancel"), cancelUpload);     
        addChild(_cancelUploadButton);        
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

    protected function createSnapshotControls () :void
    {
        // take gallery image
        _takeGalleryImage = new CommandCheckBox(Msgs.WORLD.get("b.snap_gallery"), 
            enforceUIInterlocks);
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
        addChild(_showOccs);
        
        // show chat
        _showChat = new CommandCheckBox(Msgs.WORLD.get("b.snap_overlays"), takeNewSnapshot);
        _showChat.selected = true;
        addChild(_showChat);

        addChild(new CommandButton(Msgs.WORLD.get("b.snap_update"), takeNewSnapshot));

        _preview = new Image();
        _preview.source = new BitmapAsset(sceneThumbnail.bitmap);
        addChild(_preview);

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
