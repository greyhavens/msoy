//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.BitmapData;

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;

import flash.external.ExternalInterface;

import flash.utils.ByteArray;

import mx.core.Application;
import mx.core.UIComponent;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.ViewStack;

import mx.controls.Label;

import com.adobe.images.JPGEncoder;

import com.threerings.util.Log;
import com.threerings.util.ParameterUtil;
import com.threerings.util.ValueEvent;

import com.threerings.flash.CameraSnapshotter;

import com.threerings.flex.CommandLinkButton;

import com.threerings.msoy.applets.image.CameraSnapshotControl;
import com.threerings.msoy.applets.image.ImageContext;

import com.threerings.msoy.applets.net.MediaUploader;

import com.threerings.msoy.applets.util.Downloader;

/**
 * Standalone ImageEditor. Works with imageeditor.mxml.
 */
public class ImageControls
{
    public const log :Log = Log.getLog(this);

    public function ImageControls (app :Application, viewStack :ViewStack)
    {
        _ctx = new ImageContext(app, viewStack);
        ParameterUtil.getParameters(app, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        _params = params;
        _size = new SizeRestriction(NaN, NaN,
            Number(params["maxWidth"]), Number(params["maxHeight"]));

        var options :ImageControlOptions = new ImageControlOptions(_ctx);
        options.addEventListener(Event.COMPLETE, handleOptionChoice);
        const url :String = params["url"] as String;
        options.open(url != null);
    }

    protected function handleOptionChoice (event :ValueEvent) :void
    {
        switch (event.value) {
        case ImageControlOptions.CANCEL:
            close();
            break;

        case ImageControlOptions.EDIT:
            var downloader :Downloader = new Downloader(_ctx);
            downloader.addEventListener(Event.COMPLETE, handleDownloadComplete);
            var url :String = _params["url"] as String;
            downloader.startDownload(url, "image" + url.substring(url.lastIndexOf(".") + 1));
            break;

        case ImageControlOptions.NEW:
            var newImage :NewImageDialog = new NewImageDialog(_ctx, _size);
            newImage.addEventListener(Event.COMPLETE, handleNewImage);
            newImage.addEventListener(Event.CANCEL, close);
            break;

        case ImageControlOptions.CAMERA:
            new CameraSnapshotControl(_ctx, _ctx.getApplication(), editImage); //snapshotDone);
            break;
        }
    }

    protected function handleDownloadComplete (event :ValueEvent) :void
    {
        if (event.value == null) {
            close();

        } else {
            const result :Array = event.value as Array;
            _filename = String(result[0]);
            editImage(result[1]);
        }
    }

    protected function handleNewImage (event :ValueEvent) :void
    {
        editImage(event.value);
    }

    protected function editImage (image :Object = null) :void
    {
        const manip :ImageManipulator = new ImageManipulator(
            _ctx, _ctx.getViewWidth(), _ctx.getViewHeight(), _size);
        manip.addEventListener(Event.CLOSE, handleImageClose);
        _ctx.pushView(manip);
        if (image != null) {
            manip.setImage(image);
        }
    }

    protected function handleImageClose (event :ValueEvent) :void
    {
        _ctx.popView();
        if (Boolean(event.value)) {
            // do a save
            var result :Array = ImageManipulator(event.target).getImage();
            var filename :String = _filename;
            if (result.length > 1) {
                filename = "image." + result[1];
            }
            doUpload(ByteArray(result[0]), filename);

        } else {
            close();
        }
    }

//    protected function snapshotDone (bitmapData :BitmapData) :void
//    {
//        doUpload(new JPGEncoder().encode(bitmapData), "snapshot.jpg");
//    }
//
    protected function doUpload (bytes :ByteArray, filename :String) :void
    {
        var uploader :MediaUploader = new MediaUploader(_ctx, _params["server"], _params["auth"]);
        uploader.addEventListener(Event.COMPLETE, handleUploadComplete);
        //uploader.addEventListener(ProgressEvent.PROGRESS, handleUploadProgress);
        uploader.addEventListener(IOErrorEvent.IO_ERROR, handleUploadError);
        uploader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleUploadError);
        uploader.upload(_params["mediaIds"], filename, bytes);
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        trace("Oh noes: " + event.text);
        // TODO
    }

    protected function handleUploadComplete (event :Event) :void
    {
        var uploader :MediaUploader = event.target as MediaUploader;
        var result :Object = uploader.getResult();
        uploader.close();

        for (var mediaId :String in result) {
            var data :Object = result[mediaId];
            try {
                ExternalInterface.call("setHash", mediaId, data.hash, data.mimeType,
                    data.constraint, data.width, data.height);
            } catch (err :Error) {
                log.warning("Error setting hash", err);
            }
        }

        // and now close everything
        close();
    }

    /**
     * Close everything down and return the user to the editing interface.
     */
    protected function close (... ignored) :void
    {
        try {
            ExternalInterface.call("closeImageEditor");
        } catch (err :Error) {
            log.warning("Unable to close image editor", err);
        }
    }

    protected var _ctx :ImageContext;

    protected var _params :Object;

    /** A filename, used only if the image editor doesn't override. */
    protected var _filename :String;

    protected var _size :SizeRestriction;
}
}
