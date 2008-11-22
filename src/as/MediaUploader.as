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
import flash.net.URLRequest;
import flash.net.URLRequestMethod;
import flash.net.URLVariables;

import flash.display.Sprite;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.SimpleTextButton;

import com.threerings.msoy.client.DeploymentConfig;

[SWF(width="320", height="30")]
public class MediaUploader extends Sprite
{
    public function MediaUploader ()
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
        _fileRef.addEventListener(DataEvent.UPLOAD_COMPLETE_DATA, handleUploadComplete);

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
        var request :URLRequest = new URLRequest(DeploymentConfig.serverURL + "uploadsvc");
        request.contentType = "multipart/form-data; boundary=" + "ooooooTwadddle0000";
        request.method = URLRequestMethod.POST;

        var variables :URLVariables = new URLVariables();
        variables["client"] = "mchooser";
        variables["auth"] = _params["authToken"];

        request.data = variables;

        _fileRef.upload(request, String(_params["mediaIds"]));

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

    /**
     * Handles COMPLETE -or- CANCEL.
     */
    protected function handleUploadComplete (event :Event) :void
    {
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
