//
// $Id$

package com.threerings.msoy.room.client.snapshot {

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;

import flash.geom.Rectangle;

import flash.net.FileReference;
import flash.net.URLRequest;

import flash.system.Capabilities;

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

    protected var CAN_SNAP :Boolean = true; // TODO: TEMP

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

    /**
     * Return true if the controller should download a gallery image.
     */
    public function get shouldDownloadImage () :Boolean
    {
        return _downloadImage.selected;
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
        return (shouldSaveGalleryImage || shouldDownloadImage || shouldSaveSceneThumbnail) &&
            CAN_SNAP;
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
        bar.label = Msgs.WORLD.get("m.snap_progress");
        addChild(bar);
        _progressLabel = new Label();
        _progressLabel.text = Msgs.WORLD.get("m.snap_upload_starting");
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

        // cancel any in-progress upload
        _ctrl.cancelUpload();
        
        // close the panel
        close();
    }

    protected function createSnapshotControls () :void
    {
        if (int(String(Capabilities.version.split(" ")[1]).split(",")[0]) > 9) {
            addChild(FlexUtil.createLabel(
                "Snapshots are currently broken in Flash Player 10, we'll fix them soon.",
                "attentionLabel"));
            CAN_SNAP = false;
        }

        var hPan :HBox = new HBox();
        _showOccs = new CommandCheckBox(Msgs.WORLD.get("b.snap_occs"), takeNewSnapshot);
        _showOccs.selected = true;
        hPan.addChild(_showOccs);
        _showChat = new CommandCheckBox(Msgs.WORLD.get("b.snap_overlays"), takeNewSnapshot);
        _showChat.selected = true;
        hPan.addChild(_showChat);
        addChild(hPan);

        hPan = new HBox();
        hPan.addChild(new CommandButton(Msgs.WORLD.get("b.snap_update"), takeNewSnapshot));
        hPan.addChild(FlexUtil.createLabel(Msgs.WORLD.get("l.snap_preview")));
        addChild(hPan);

        _preview = new Image();
        _preview.source = new BitmapAsset(sceneThumbnail.bitmap);
        addChild(_preview);

        addChild(FlexUtil.createLabel(Msgs.WORLD.get("m.snap_save_opts")));
        _takeGalleryImage = new CommandCheckBox(Msgs.WORLD.get("b.snap_gallery"), 
            enforceUIInterlocks);
        _takeGalleryImage.selected = true;
        addChild(_takeGalleryImage);
        _downloadImage = new CommandCheckBox(Msgs.WORLD.get("b.snap_download"),
            enforceUIInterlocks);
        addChild(_downloadImage);
        // only add the button to take the canonical snapshot if it's enabled.
        if (_sceneThumbnailPermitted) {
            _useAsSceneThumbnail = new CommandCheckBox(Msgs.WORLD.get("b.snap_scene_thumbnail"), 
                enforceUIInterlocks);
            _useAsSceneThumbnail.selected = false;
            addChild(_useAsSceneThumbnail);
        }

        addButtons(OK_BUTTON, CANCEL_BUTTON);
        enforceUIInterlocks();        
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON) {
            upload();
        } else {
            super.buttonClicked(buttonId);
        }
    }

    /**
     * Begin the upload process, much of which happens asynchronously.
     */
    protected function upload () :void
    {
        showProgressBar();

        if (shouldSaveSceneThumbnail) {
            _progressLabel.text = Msgs.WORLD.get("m.snap_upload_thumb");
            sceneThumbnail.encodeAndUpload(_ctrl.upload,
                [ SnapshotController.SCENE_THUMBNAIL_SERVICE, false, uploadGalleryImage ]);

        } else {
            uploadGalleryImage();
        }
    }

    /**
     * Second stage of the upload process.
     */
    protected function uploadGalleryImage (... ignored) :void
    {
        if (shouldSaveGalleryImage || shouldDownloadImage) {
            _progressLabel.text = Msgs.WORLD.get("m.snap_upload_snap");
            galleryImage.encodeAndUpload(_ctrl.upload,
                [ SnapshotController.SCENE_SNAPSHOT_SERVICE, shouldSaveGalleryImage, doDownload ]);

        } else {
            doDownload(null);
        }
    }

    /**
     * Called if uploading or downloading failed.
     */
    public function reportError (message :String) :void
    {
        _progressLabel.text = message;
        _cancelUploadButton.label = Msgs.GENERAL.get("b.ok");
    }
    
    /**
     * Called when uploading is complete.
     */
    protected function doDownload (downloadURL :String) :void
    {
        if (shouldDownloadImage && (downloadURL != null)) {
            _progressLabel.text = Msgs.WORLD.get("m.snap_download");
            _downloadRef = new FileReference();
            _downloadRef.addEventListener(Event.CANCEL, handleDownloadStopEvent);
            _downloadRef.addEventListener(Event.COMPLETE, handleDownloadStopEvent);
            _downloadRef.addEventListener(SecurityErrorEvent.SECURITY_ERROR,
                handleDownloadStopEvent);
            _downloadRef.addEventListener(IOErrorEvent.IO_ERROR, handleDownloadStopEvent);
            _downloadRef.download(new URLRequest(downloadURL), "snapshot.jpg");

        } else {
            // done at this point so we can close the panel
            close();        
        }
    }

    protected function handleDownloadStopEvent (event :Event) :void
    {
        if (event is ErrorEvent) {
            reportError(Msgs.WORLD.get("e.snap_download", ErrorEvent(event).text));
        } else {
            close();
        }
    }

    protected var _sceneThumbnailPermitted :Boolean;

    protected var _preview :Image;
    protected var _view :RoomView;
    protected var _ctrl :SnapshotController;

    protected var _downloadRef :FileReference;

    // UI Elements
    protected var _showOccs :CommandCheckBox;
    protected var _showChat :CommandCheckBox;
    protected var _useAsSceneThumbnail :CommandCheckBox;
    protected var _takeGalleryImage :CommandCheckBox;
    protected var _downloadImage :CommandCheckBox;

    protected var _cancelUploadButton :CommandButton;
    protected var _progressLabel :Label;    
}
}
