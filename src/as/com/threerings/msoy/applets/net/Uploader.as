//
// $Id$

package com.threerings.msoy.applets.net {

import flash.events.DataEvent;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.HTTPStatusEvent;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;

import flash.net.FileReference;
import flash.net.URLRequest;
import flash.net.URLRequestMethod;

import mx.controls.Label;
import mx.controls.ProgressBar;

import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.managers.PopUpManager;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.applets.AppletContext;
import com.threerings.msoy.client.DeploymentConfig;

/**
 * Dispatched when we're closed.
 * value- null if the upload was cancelled or in error, or
 *        [ local filename, response from server ]
 */
[Event(name="complete", type="com.threerings.util.ValueEvent")]

/**
 * Handles uploading a file to the remix upload servlet.
 */
public class Uploader extends TitleWindow
{
    public function Uploader (ctx :AppletContext, service :String, fileFilters :Array = null)
    {
        _ctx = ctx;
        _service = service;

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
        this.title = _ctx.APPLET.get("t.uploading", _fileRef.name);

        var box :VBox = new VBox();
        _label = new Label();
        _label.text = _ctx.APPLET.get("l.uploading", _fileRef.name);
        _progress = new ProgressBar();
        addChild(_progress);
        addChild(_stop = new CommandButton(_ctx.APPLET.get("b.cancel"), close));

        _progress.source = _fileRef;

        PopUpManager.addPopUp(this, _ctx.getApplication(), true);
        PopUpManager.centerPopUp(this);

        _fileRef.upload(MediaUploadUtil.createRequest(_service, _ctx.authToken));
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        FlexUtil.setVisible(_progress, false);
        _label.text = _ctx.APPLET.get("e.upload", event.text);
        _stop.label = _ctx.APPLET.get("b.ok");
        // wait to continue until the user hits "OK"
    }

    protected function handleUploadComplete (event :DataEvent) :void
    {
        var data :Array = [ _fileRef.name, event.data ];
        close(data);
    }

    protected var _ctx :AppletContext;

    protected var _service :String;

    protected var _fileRef :FileReference;

    protected var _label :Label;

    protected var _progress :ProgressBar;

    protected var _stop :CommandButton;
}
}
