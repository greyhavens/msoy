//
// $Id$

package {

import flash.events.DataEvent;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.HTTPStatusEvent;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;

import flash.external.ExternalInterface;

import flash.display.Sprite;

import flash.net.FileReference;

import flash.text.TextField;
import flash.text.TextFormat;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.SimpleTextButton;

import com.threerings.msoy.applets.net.MediaUploadUtil;

import com.threerings.msoy.client.DeploymentConfig;

[SWF(width="200", height="40")]
public class UploaderApp extends Sprite
{
    public function UploaderApp ()
    {
        this.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        _progress = new Sprite();
        addChild(_progress);

        _status = new TextField();
        _status.width = 200;
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

        _button = new SimpleTextButton(text, false, 0x000000, 0xCCCCCC, 0x000000, 2,
            new TextFormat("_sans"));
        _button.y = 15;
        _button.addEventListener(MouseEvent.CLICK, handler);
        addChild(_button);
    }

    protected function displayProgress (progress :Number) :void
    {
        _progress.graphics.clear();
        if (progress > 0) {
            _progress.graphics.beginFill(0xFF00000);
            _progress.graphics.drawRect(0, 0, 199 * progress, 10);
            _progress.graphics.endFill();
        }
        _progress.graphics.lineStyle(1, 0x000000);
        _progress.graphics.drawRect(0, 0, 199, 10);
    }

    protected function handleChooseFile (event :MouseEvent) :void
    {
        // TODO: file filters based on type?
        _fileRef.browse();

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
        setStatus("Error uploading: " + event.text);
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
                ExternalInterface.call("setHash", mediaId, data.hash, data.mimeType,
                    data.constraint, data.width, data.height);
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

    protected var _button :SimpleTextButton;
}
}
