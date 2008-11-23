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

import flash.net.FileReference;

import flash.display.Sprite;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.SimpleTextButton;

import com.threerings.msoy.applets.net.MediaUploadUtil;

import com.threerings.msoy.client.DeploymentConfig;

[SWF(width="200", height="30")]
public class UploaderApp extends Sprite
{
    public function UploaderApp ()
    {
        this.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

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
        showButton("Upload file", handleChooseFile);
    }

    protected function showButton (text :String, handler :Function) :void
    {
        if (_button != null) {
            removeChild(_button);
        }

        _button = new SimpleTextButton(text, false);
        _button.addEventListener(MouseEvent.CLICK, handler);
        addChild(_button);
    }

    protected function handleChooseFile (event :MouseEvent) :void
    {
        // TODO: file filters based on type?
        _fileRef.browse();

        removeChild(_button);
        _button = null;
    }

    protected function handleCancelUpload (event :MouseEvent) :void
    {
        cancelUpload();
        showUploadButton();
    }

    protected function handleSelectCancelled (event :Event) :void
    {
        showUploadButton();
    }

    protected function handleFileSelected (event :Event) :void
    {
        _fileRef.upload(MediaUploadUtil.createRequest("uploadsvc", String(_params["auth"])),
            String(_params["mediaIds"]));

        showButton("Cancel upload", handleCancelUpload);
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        // TODO
        trace("Jimminy: " + event.text);
        showUploadButton();
    }

    protected function handleProgress (event :ProgressEvent) :void
    {
        const progress :Number = event.bytesLoaded / event.bytesTotal;
        // TODO
        trace("Progress: " + progress);
    }

    protected function handleUploadCompleteData (event :DataEvent) :void
    {
        trace("complete data: " + event.data);
    }

    protected function handleUploadComplete (event :Event) :void
    {
        trace("complete");
        showUploadButton();
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

    protected var _params :Object;

    protected var _fileRef :FileReference;

    protected var _button :SimpleTextButton;
}
}
