//
// $Id$

package com.threerings.msoy.applets {

import flash.display.Sprite;
import flash.events.DataEvent;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.HTTPStatusEvent;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;
import flash.external.ExternalInterface;
import flash.net.FileFilter;
import flash.net.FileReference;
import flash.system.Security;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import com.threerings.util.ParameterUtil;

import com.threerings.text.TextFieldUtil;

import com.threerings.ui.SimpleSkinButton;

import com.threerings.msoy.applets.net.MediaUploadUtil;
import com.threerings.msoy.client.DeploymentConfig;

[SWF(width="200", height="40")]
public class UploaderApp extends Sprite
{
    public static const WIDTH :int = 200;
    public static const HEIGHT :int = 40;

    public function UploaderApp ()
    {
        this.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        _progress = new Sprite();
        addChild(_progress);

        _status = TextFieldUtil.createField("", { autoSize: TextFieldAutoSize.LEFT },
            { font: "_sans", size: 12, color: 0x000000 });
        _status.width = WIDTH;
        addChild(_status);

        _fileRef = new FileReference();
        _fileRef.addEventListener(Event.SELECT, handleFileSelected);
        _fileRef.addEventListener(Event.CANCEL, handleSelectCancelled);
        _fileRef.addEventListener(IOErrorEvent.IO_ERROR, handleUploadError);
        _fileRef.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleUploadError);
//        _fileRef.addEventListener(HTTPStatusEvent.HTTP_STATUS, dispatchEvent);
//        _fileRef.addEventListener(Event.OPEN, dispatchEvent);
        _fileRef.addEventListener(ProgressEvent.PROGRESS, handleProgress);
        _fileRef.addEventListener(DataEvent.UPLOAD_COMPLETE_DATA, handleUploadCompleteData);
        _fileRef.addEventListener(Event.COMPLETE, handleUploadComplete);

        Security.loadPolicyFile(DeploymentConfig.crossDomainURL);

        ParameterUtil.getParameters(this, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        _params = params;

        showUploadButton();
    }

    protected function showUploadButton () :void
    {
        _progress.graphics.clear();
        showButton("Upload a new file", handleChooseFile);
    }

    protected function setStatus (text :String) :void
    {
        _status.text = text;
    }

    protected function showButton (text :String, handler :Function) :void
    {
        if (_button != null) {
            removeChild(_button);
        }

        _button = new SimpleSkinButton(BUTTON_SKIN, text, { autoSize: TextFieldAutoSize.CENTER },
            { font: "_sans", color: 0x000000, size: 12 }, 8, BUTTON_HEIGHT, 1, 0, 0x404040);
        _button.y = HEIGHT / 2;
        _button.addEventListener(MouseEvent.CLICK, handler);
        addChild(_button);
    }

    protected function displayProgress (progress :Number) :void
    {
        _progress.graphics.clear();
        if (progress > 0) {
            _progress.graphics.beginFill(0xFF00000);
            _progress.graphics.drawRect(0, 0, (WIDTH - 1) * progress, 15);
            _progress.graphics.endFill();
        }
        _progress.graphics.lineStyle(1, 0x000000);
        _progress.graphics.drawRect(0, 0, WIDTH - 1, 15);
    }

    protected function handleChooseFile (event :MouseEvent) :void
    {
        // we put all the filetypes in as one
        var fileTypes :String = _params["filetypes"] as String;
        _fileRef.browse((fileTypes == null) ? null : [ new FileFilter("files", fileTypes) ]);

        setStatus("Choose the file to upload...");
        removeChild(_button);
        _button = null;
    }

    protected function handleCancelUpload (event :MouseEvent) :void
    {
        cancelUpload();
        showUploadButton();
        setStatus("Upload cancelled");
    }

    protected function handleSelectCancelled (event :Event) :void
    {
        showUploadButton();
        setStatus("");
    }

    protected function handleFileSelected (event :Event) :void
    {
        setStatus("");
        displayProgress(0);
        _fileRef.upload(MediaUploadUtil.createRequest("uploadsvc", String(_params["auth"])),
            String(_params["mediaIds"]));

        showButton("Cancel upload", handleCancelUpload);
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        trace("Full upload error: " + event.text);
        if (-1 != event.text.indexOf("2038")) { // super common error
            setStatus("Upload error: file too big, or server unavailable.");
        } else {
            setStatus("Error uploading: " + event.text);
        }
        showUploadButton();
    }

    protected function handleProgress (event :ProgressEvent) :void
    {
        displayProgress(event.bytesLoaded / event.bytesTotal);
    }

    protected function handleUploadCompleteData (event :DataEvent) :void
    {
        if (ExternalInterface.available) {
            var result :Object = MediaUploadUtil.parseResult(event.data);
            for (var mediaId :String in result) {
                var data :Object = result[mediaId];
                ExternalInterface.call("setHash", mediaId, _fileRef.name, data.hash, data.mimeType,
                    data.constraint, /*data.expiration, data.signature,*/ data.width, data.height);
            }
        }
    }

    protected function handleUploadComplete (event :Event) :void
    {
        showUploadButton();
        setStatus("Upload complete");
    }

    protected function cancelUpload () :void
    {
        try {
            _fileRef.cancel();
        } catch (e :Error) {
            // ignore
        }
    }

    protected function handleUnload (event :Event) :void
    {
        cancelUpload();
    }

    protected var _progress :Sprite;

    protected var _status :TextField;

    protected var _params :Object;

    protected var _fileRef :FileReference;

    protected var _button :SimpleSkinButton;

    protected static const BUTTON_HEIGHT :int = 19;

    [Embed(source="../../../../../../pages/images/ui/button_middle.png")]
    protected static const BUTTON_SKIN :Class;
}
}
