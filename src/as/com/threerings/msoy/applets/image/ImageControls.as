//
// $Id$

package com.threerings.msoy.applets.image {

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.external.ExternalInterface;
import flash.utils.ByteArray;

import mx.containers.ViewStack;
import mx.core.Application;

import com.threerings.util.Log;
import com.threerings.util.ParameterUtil;
import com.threerings.util.ValueEvent;

import com.threerings.msoy.applets.image.CameraSnapshotControl;
import com.threerings.msoy.applets.image.ImageContext;
import com.threerings.msoy.applets.net.Downloader;
import com.threerings.msoy.applets.net.MediaUploader;

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
        _size = new SizeRestriction(Number(params["reqWidth"]), Number(params["reqHeight"]),
            Number(params["maxWidth"]), Number(params["maxHeight"]));

        const url :String = params["url"] as String;
        if (url != null) {
            var downloader :Downloader = new Downloader(_ctx);
            downloader.addEventListener(Event.COMPLETE, handleDownloadComplete);
            downloader.startDownload(url, _ctx.changeFilename(url, "image"));

        } else if (null != params["takeSnapshot"]) {
            new CameraSnapshotControl(_ctx, _ctx.getApplication(), handleCameraDone);

        } else {
            var newImage :NewImageDialog = new NewImageDialog(_ctx, _size);
            newImage.addEventListener(Event.COMPLETE, handleNewImage);
            newImage.addEventListener(Event.CANCEL, close);
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

    protected function handleCameraDone (image :Object) :void
    {
        if (image != null) {
            editImage(image);
        } else {
            close();
        }
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

    protected function doUpload (bytes :ByteArray, filename :String) :void
    {
        var uploader :MediaUploader = new MediaUploader(_ctx);
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
                ExternalInterface.call("setHash", mediaId, data.filename, data.hash, data.mimeType,
                    data.constraint, /*data.expiration, data.signature,*/ data.width, data.height);
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
