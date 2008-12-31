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
    /** This is the maximum bitmap dimension, a flash limitation. */
    public static const MAX_BITMAP_DIM :int = 2880; // TODO: this is larger in FP10!

    public var galleryImage :Snapshot;
    public var sceneThumbnail :Snapshot;

    public function SnapshotPanel (ctx :WorldContext)
    {
        super(ctx, Msgs.WORLD.get("t.snap"));

        setStyle("horizontalAlign", "left");

        _view = ctx.getPlaceView() as RoomView;

        // if the user is permitted to manage the room then enable the taking of canonical snapshots
        _sceneThumbnailPermitted = _view.getRoomController().canManageRoom();

        // for the canonical image, we create a new framer that centers the image within the frame,
        // introducing black bars if necessary.
        sceneThumbnail = Snapshot.createThumbnail(ctx, _view,
            handleEncodingComplete, handleUploadError);

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
        galleryImage = new Snapshot(ctx, false, _view, galFramer, galWidth, galHeight,
            handleEncodingComplete, handleUploadError);
        open();
    }

    override public function close () :void
    {
        // cancel any encoding processes that may be running.
        galleryImage.cancelAll();
        sceneThumbnail.cancelAll();

        // close the panel
        super.close();
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
        return (_takeGalleryImage != null) && _takeGalleryImage.selected;
    }

    /**
     * Return true if the controller should download a gallery image.
     */
    public function get shouldDownloadImage () :Boolean
    {
        return _downloadImage.selected;
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        createSnapshotControls();
        showCloseButton = true;
    }

    protected function createSnapshotControls () :void
    {
        const isGuest :Boolean = _ctx.getMyName().isGuest();

        var hPan :HBox = new HBox();
        _showOccs = new CommandCheckBox(Msgs.WORLD.get("b.snap_occs"), takeNewSnapshot);
        _showOccs.selected = true;
        hPan.addChild(_showOccs);
        _showChat = new CommandCheckBox(Msgs.WORLD.get("b.snap_chat"), takeNewSnapshot);
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
        // only add the button to take the canonical snapshot if it's enabled.
        if (_sceneThumbnailPermitted) {
            _useAsSceneThumbnail = new CommandCheckBox(Msgs.WORLD.get("b.snap_scene_thumbnail"),
                enforceUIInterlocks);
            _useAsSceneThumbnail.selected = false;
            addChild(_useAsSceneThumbnail);
        }
        if (!isGuest) {
            _takeGalleryImage = new CommandCheckBox(Msgs.WORLD.get("b.snap_gallery"),
                enforceUIInterlocks);
            _takeGalleryImage.selected = true;
            addChild(_takeGalleryImage);
        }
        _downloadImage = new CommandCheckBox(Msgs.WORLD.get("b.snap_download"),
            enforceUIInterlocks);
        _downloadImage.selected = isGuest;
        addChild(_downloadImage);

        addButtons(OK_BUTTON, CANCEL_BUTTON);

        // update the snaps
        takeNewSnapshot();
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
     * Should be called if the user changes an option that means we should update other UI elements.
     * Does not take a new snapshot.
     */
    protected function enforceUIInterlocks (... ignored) :void
    {
        if (shouldSaveSceneThumbnail) {
            sceneThumbnail.startEncode();
        }
        if (shouldSaveGalleryImage || shouldDownloadImage) {
            galleryImage.startEncode();
        }
        getButton(OK_BUTTON).enabled = canSave();
    }

    /**
     * We can save an image if either one or both of the image saving options are selected.
     */
    protected function canSave () :Boolean
    {
        const needGallery :Boolean = shouldSaveGalleryImage || shouldDownloadImage;
        const needThumb :Boolean = shouldSaveSceneThumbnail;
        return (needGallery || needThumb) &&
            (!needGallery || galleryImage.ready) && (!needThumb || sceneThumbnail.ready);
    }

    protected function handleEncodingComplete (event :Event) :void
    {
        trace("encoding complete.");
        enforceUIInterlocks();
    }

    protected function takeNewSnapshot (... ignored) :void
    {
        var occs :Boolean = _showOccs.selected;
        if (!occs) {
            _showChat.selected = false;
        }
        _showChat.enabled = occs;

        sceneThumbnail.updateSnapshot(occs, _showChat.selected, shouldSaveSceneThumbnail);
        galleryImage.updateSnapshot(occs, _showChat.selected,
            shouldSaveGalleryImage || shouldDownloadImage);
        enforceUIInterlocks();
    }

    protected function showProgressControls () :void
    {
        removeAllChildren();

        _progressBar = new ProgressBar();
        _progressBar.percentWidth = 100;
        _progressBar.indeterminate = true;
        _progressBar.mode = "manual";
        _progressBar.label = Msgs.WORLD.get("m.snap_progress");
        addChild(_progressBar);
        _progressLabel = new Label();
        _progressLabel.text = Msgs.WORLD.get("m.snap_upload");
        addChild(_progressLabel);
        _cancelUploadButton = new CommandButton(Msgs.WORLD.get("b.snap_cancel"), close);
        addChild(_cancelUploadButton);

        showCloseButton = false;
    }

    /**
     * Begin the upload process, much of which happens asynchronously.
     */
    protected function upload () :void
    {
        _waiting = 0;
        showProgressControls();

        if (shouldSaveSceneThumbnail) {
            _waiting++;
            sceneThumbnail.upload(false, handleUploadDone);

        }
        if (shouldSaveGalleryImage || shouldDownloadImage) {
            _waiting++;
            galleryImage.upload(shouldSaveGalleryImage, setupDownload);
        }

        if (_waiting == 0) {
            close();
        }
    }

    protected function handleUploadDone (... ignored) :void
    {
        if (--_waiting == 0) {
            // done at this point so we can close the panel
            close();
        }
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        reportError(Msgs.WORLD.get("e.snap_upload", event.text));
    }

    /**
     * Called if uploading or downloading failed.
     */
    protected function reportError (message :String) :void
    {
        _progressLabel.text = message;
        _cancelUploadButton.label = Msgs.GENERAL.get("b.ok");
        _cancelUploadButton.enabled = true;
    }

    /**
     * Called when uploading is complete.
     */
    protected function setupDownload (downloadURL :String) :void
    {
        if (shouldDownloadImage && (downloadURL != null)) {
            _downloadURL = downloadURL;

            _progressBar.indeterminate = false;
            _progressBar.setProgress(1, 1);
            _progressLabel.text = Msgs.WORLD.get("m.snap_download");
            _cancelUploadButton.label = Msgs.GENERAL.get("b.ok");
            _cancelUploadButton.setCallback(doDownload);

        } else {
            handleUploadDone();
        }
    }

    protected function doDownload () :void
    {
        _cancelUploadButton.setCallback(close);
        _cancelUploadButton.enabled = false;
        _progressLabel.text = Msgs.WORLD.get("m.snap_download_progress");
        _progressBar.indeterminate = true;

        _downloadRef = new FileReference();
        _downloadRef.addEventListener(Event.CANCEL, handleDownloadStopEvent);
        _downloadRef.addEventListener(Event.COMPLETE, handleDownloadStopEvent);
        _downloadRef.addEventListener(SecurityErrorEvent.SECURITY_ERROR,
            handleDownloadStopEvent);
        _downloadRef.addEventListener(IOErrorEvent.IO_ERROR, handleDownloadStopEvent);
        _downloadRef.download(new URLRequest(_downloadURL), "snapshot.jpg");
    }

    protected function handleDownloadStopEvent (event :Event) :void
    {
        if (event is ErrorEvent) {
            reportError(Msgs.WORLD.get("e.snap_download", ErrorEvent(event).text));
        } else {
            handleUploadDone();
        }
    }

    protected var _sceneThumbnailPermitted :Boolean;

    protected var _preview :Image;
    protected var _view :RoomView;

    protected var _waiting :int;
    protected var _downloadRef :FileReference;
    protected var _downloadURL :String;

    // UI Elements
    protected var _showOccs :CommandCheckBox;
    protected var _showChat :CommandCheckBox;
    protected var _useAsSceneThumbnail :CommandCheckBox;
    protected var _takeGalleryImage :CommandCheckBox;
    protected var _downloadImage :CommandCheckBox;

    protected var _cancelUploadButton :CommandButton;
    protected var _progressLabel :Label;
    protected var _progressBar :ProgressBar;
}
}
