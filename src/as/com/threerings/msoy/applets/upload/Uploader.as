//
// $Id$

package com.threerings.msoy.applets.upload {

import flash.events.DataEvent;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.HTTPStatusEvent;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;

import flash.net.FileFilter;
import flash.net.FileReference;
import flash.net.URLRequest;
import flash.net.URLRequestMethod;

import mx.controls.Label;
import mx.controls.ProgressBar;

import mx.containers.HBox;
import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.core.Application;

import mx.managers.PopUpManager;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.utils.Base64Decoder;

public class Uploader extends TitleWindow
{
    public function Uploader (serverURL :String, fileFilters :Array = null)
    {
        _serverURL = serverURL;

        _fileRef = new FileReference();
        _fileRef.addEventListener(Event.SELECT, handleFileSelected);
        _fileRef.addEventListener(Event.CANCEL, handleFileCancelled);
        _fileRef.addEventListener(IOErrorEvent.IO_ERROR, handleUploadError);
        _fileRef.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleUploadError);
//        _fileRef.addEventListener(HTTPStatusEvent.HTTP_STATUS, dispatchEvent);
//        _fileRef.addEventListener(Event.OPEN, dispatchEvent);
//        _fileRef.addEventListener(ProgressEvent.PROGRESS, dispatchEvent);
        _fileRef.addEventListener(DataEvent.UPLOAD_COMPLETE_DATA, handleUploadComplete);

        _fileRef.browse(fileFilters);
    }

    public function close (returnValue :Object = null) :void
    {
        try {
            _fileRef.cancel();
        } catch (err :Error) {
            // ignore
        }

        PopUpManager.removePopUp(this);

        dispatchEvent(new ValueEvent(Event.COMPLETE, returnValue));
    }

    protected function handleFileCancelled (event :Event) :void
    {
        close();
    }

    protected function handleFileSelected (event :Event) :void
    {
        this.title = "Uploading " + _fileRef.name;

        var box :VBox = new VBox();
        _label = new Label();
        _label.text = "Uploading " + _fileRef.name;
        _progress = new ProgressBar();
        addChild(_progress);
        addChild(_stop = new CommandButton("Cancel", close));

        _progress.source = _fileRef;

        PopUpManager.addPopUp(this, Application(Application.application), true);
        PopUpManager.centerPopUp(this);

        var req :URLRequest = new URLRequest(_serverURL + "echouploadsvc");
        req.method = URLRequestMethod.POST;
        _fileRef.upload(req);
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        _progress.visible = false;
        _progress.includeInLayout = false;

        _label.text = "Error uploading: " + event.text;
        _stop.label = "OK";
        // wait to continue until the user hits "OK"
    }

    protected function handleUploadComplete (event :DataEvent) :void
    {
        var decoder :Base64Decoder = new Base64Decoder();
        decoder.decode(event.data);
        // automatically close with the completed data
        close([ _fileRef.name, decoder.toByteArray() ]);
    }

    protected var _serverURL :String;

    protected var _fileRef :FileReference;

    protected var _label :Label;

    protected var _progress :ProgressBar;

    protected var _stop :CommandButton;
}
}
