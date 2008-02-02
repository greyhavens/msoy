package com.threerings.msoy.applets.remixer {

import flash.display.BitmapData;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.ErrorEvent;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;

import flash.external.ExternalInterface;

import flash.utils.ByteArray;

import com.adobe.images.JPGEncoder;

import com.whirled.remix.data.EditableDataPack;

import com.threerings.util.ParameterUtil;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.flash.CameraSnapshotter;
import com.threerings.flash.SimpleTextButton;

import com.threerings.msoy.utils.Base64Sender;

import com.threerings.msoy.applets.net.MediaUploader;

[SWF(width="540", height="450")]
public class Remixer extends Sprite
{
    public function Remixer ()
    {
        ParameterUtil.getParameters(this, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        _params = params;
        var media :String = params["media"] as String;

        _pack = new EditableDataPack(media);
        _pack.addEventListener(Event.COMPLETE, handlePackComplete);
        _pack.addEventListener(ErrorEvent.ERROR, handlePackError);
    }

    protected function handlePackComplete (event :Event) :void
    {
        updatePreview();

        var btn :SimpleTextButton = new SimpleTextButton("change Eye color");
        btn.addEventListener(MouseEvent.CLICK, changeEyeColor);
        addChild(btn);

        btn = new SimpleTextButton("change texture");
        btn.addEventListener(MouseEvent.CLICK, changeTexture);
        btn.y = 50;
        addChild(btn);

        btn = new SimpleTextButton("Save");
        btn.addEventListener(MouseEvent.CLICK, commit);
        btn.y = 400;
        addChild(btn);
//
//        var dataFields :Array = _pack.getDataFields();
////        trace(StringUtil.toString(dataFields));
//        for each (var name :String in dataFields) {
//            var obj :Object = _pack.getDataEntry(name);
//            trace("" + name + ":");
//            for (var propName :String in obj) {
//                trace("   " + propName + " -> " + obj[propName]);
//            }
//        }
//        var fileFields :Array = _pack.getFileFields();
//        trace(StringUtil.toString(fileFields));
    }

    protected function handlePackError (event :ErrorEvent) :void
    {
        trace("Error loading: " + event.text)
    }

    protected function changeEyeColor (... ignored) :void
    {
        var color :uint = uint(Math.random() * 0xFFFFFF)
        _pack.setData("eyeColor", color);
        updatePreview();
    }

    protected function changeTexture (... ignored) :void
    {
        if (_snapper != null) {
            return;
        }

        _snapper = new CameraSnapshotter(300, 300);
        _snapper.addEventListener(Event.COMPLETE, handleSnapshotArrived);
        _snapper.x = 100;
        addChild(_snapper);
    }

    protected function handleSnapshotArrived (event :ValueEvent) :void
    {
        removeChild(_snapper);
        _snapper = null;

        var bmp :BitmapData = event.value as BitmapData;
        if (bmp == null) {
            return;
        }

        var encoder :JPGEncoder = new JPGEncoder(75);
        _pack.replaceFile("texture", "mix-texture.jpg", encoder.encode(bmp));
        updatePreview();
    }

    protected function updatePreview () :void
    {
        // send the bytes to our previewer
        var b64 :Base64Sender = new Base64Sender("remixPreview", "setMediaBytes");
        b64.sendBytes(_pack.serialize());
    }

    protected function commit (... ignored) :void
    {
        var uploader :MediaUploader = new MediaUploader(_params["server"], _params["auth"]);
        uploader.addEventListener(Event.COMPLETE, handleUploadComplete);
        uploader.addEventListener(ProgressEvent.PROGRESS, handleUploadProgress);
        uploader.addEventListener(IOErrorEvent.IO_ERROR, handleUploadError);
        uploader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleUploadError);
        uploader.upload(_params["mediaId"], "datapack.zip", _pack.serialize());
    }

    protected function handleUploadProgress (event :ProgressEvent) :void
    {
        // TODO
        // unfortunately, it seems that the uploader doesn't show upload progress, only
        // the progress of downloading the data back from the server.

        //trace(":: progress " + (event.bytesLoaded * 100 / event.bytesTotal).toPrecision(3));
    }

    protected function handleUploadComplete (event :Event) :void
    {
        var uploader :MediaUploader = event.target as MediaUploader;

        var result :Object = uploader.getResult();
        trace("Got result: " + result);

        if (ExternalInterface.available) {
            ExternalInterface.call("setHash", result.mediaId, result.hash, result.mimeType,
                result.constraint, result.width, result.height);
        }
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        // TODO
        trace("Oh noes! : " + event.text);
    }

    protected var _pack :EditableDataPack;

    protected var _snapper :CameraSnapshotter;

    protected var _params :Object;
}
}
