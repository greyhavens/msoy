//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.BitmapData;

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;

import flash.utils.ByteArray;

import mx.core.Application;
import mx.core.UIComponent;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.ViewStack;

import mx.controls.Label;

import com.adobe.images.JPGEncoder;

import com.threerings.util.ParameterUtil;
import com.threerings.util.ValueEvent;

import com.threerings.flash.CameraSnapshotter;

import com.threerings.flex.CommandLinkButton;

import com.threerings.msoy.applets.image.CameraSnapshotControl;
import com.threerings.msoy.applets.image.ImageContext;

import com.threerings.msoy.applets.net.MediaUploader;

import com.threerings.msoy.applets.util.Downloader;

public class ImageControls
{
    public function ImageControls (app :Application, viewStack :ViewStack)
    {
        _ctx = new ImageContext(app, viewStack);
        ParameterUtil.getParameters(app, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        _params = params;

        _size = new SizeRestriction(NaN, NaN, Number(params["width"]), Number(params["height"]));
        const url :String = params["url"] as String;

        // TODO: if url is not null, they get 3 choices: edit, new image, camera
        // If the url is null, they only get new image and camera.
        if (url != null) {
            var downloader :Downloader = new Downloader(_ctx);
            downloader.addEventListener(Event.COMPLETE, handleDownloadComplete);
            downloader.startDownload(url, "image" + url.substring(url.lastIndexOf(".") + 1));

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
            editImage((event.value as Array)[1]);
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
        trace("Image closed: Save?: " + Boolean(event.value));
        if (Boolean(event.value)) {
            // do a save
            var result :Array = ImageManipulator(event.target).getImage();
            // TODO: saving
        }
    }

    protected function handleTakeSnapshot () :void
    {
        new CameraSnapshotControl(_ctx, _ctx.getApplication(), snapshotDone);
    }

    protected function snapshotDone (bitmapData :BitmapData) :void
    {
        var encoder :JPGEncoder = new JPGEncoder();
        var jpg :ByteArray = encoder.encode(bitmapData);

        var uploader :MediaUploader = new MediaUploader(_ctx, _params["server"], _params["auth"]);
        uploader.addEventListener(Event.COMPLETE, handleUploadComplete);
        //uploader.addEventListener(ProgressEvent.PROGRESS, handleUploadProgress);
        uploader.addEventListener(IOErrorEvent.IO_ERROR, handleUploadError);
        uploader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleUploadError);
        uploader.upload(_params["mediaIds"], "snapshot.jpg", jpg);
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
            // TODO
            trace("hash for media " + mediaId + ": " + data.hash + ", " + data.mimeType);
        }
    }

    /**
     * Close everything down and return the user to the editing interface.
     */
    protected function close (... ignored) :void
    {
        trace("Right about here we should close");
        // TODO
    }

    protected var _ctx :ImageContext;

    protected var _params :Object;

    protected var _size :SizeRestriction;

    protected var _uploader :MediaUploader;
}
}
