//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.display.LoaderInfo;

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

import flash.utils.ByteArray;

import mx.controls.ButtonBar;
import mx.controls.Image;
import mx.controls.Label;
import mx.controls.ProgressBar;

import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.core.FlexLoader;

import mx.managers.PopUpManager;

import com.threerings.flex.CommandButton;

import com.whirled.remix.data.EditableDataPack;

import com.threerings.msoy.utils.Base64Decoder;

public class PopupFilePreview extends TitleWindow
{
    public function PopupFilePreview (
        parent :FileEditor, name :String, pack :EditableDataPack, serverURL :String)
    {
        _parent = parent;
        _name = name;
        _pack = pack;
        _serverURL = serverURL;

        this.title = name;

        var box :VBox = new VBox();
        addChild(box);
        _image = new Image();
        _image.maxWidth = 300;
        _image.maxHeight = 300;
        box.addChild(_image);

        setImage(pack.getFile(name));

        var buttonBar :ButtonBar = new ButtonBar();
        buttonBar.addChild(new CommandButton("Upload...", handleChooseFile));
        box.addChild(buttonBar);

        buttonBar = new ButtonBar();
        buttonBar.addChild(new CommandButton("OK", close, true));
        buttonBar.addChild(new CommandButton("Cancel", close, false));
        box.addChild(buttonBar);

        box.addChild(_progress = new ProgressBar());
        showProgress();

        PopUpManager.addPopUp(this, parent, true);
        PopUpManager.centerPopUp(this);
    }

    public function setImage (bytes :ByteArray) :void
    {
        var l :FlexLoader = new FlexLoader();
        l.contentLoaderInfo.addEventListener(Event.COMPLETE, handleFlexLoaderComplete);
        l.loadBytes(bytes);
    }

    protected function handleFlexLoaderComplete (event :Event) :void
    {
        _image.source = LoaderInfo(event.target).loader;
        PopUpManager.centerPopUp(this);
    }

    protected function close (save :Boolean) :void
    {
        if (save && _bytes != null) {
            trace("Saving " + _filename + " (" + _bytes.length + ")");
            _parent.updateValue(_filename, _bytes);
        }

        PopUpManager.removePopUp(this);
    }

    protected function handleChooseFile () :void
    {
        if (_fileRef == null) {
            _fileRef = new FileReference();
            _fileRef.addEventListener(Event.SELECT, handleFileSelected);
            _fileRef.addEventListener(IOErrorEvent.IO_ERROR, handleUploadError);
            _fileRef.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleUploadError);
            _fileRef.addEventListener(Event.COMPLETE, handleUploadEvent);
            _fileRef.addEventListener(HTTPStatusEvent.HTTP_STATUS, handleUploadEvent);
            _fileRef.addEventListener(Event.OPEN, handleUploadEvent);
            _fileRef.addEventListener(ProgressEvent.PROGRESS, handleUploadProgress);
            _fileRef.addEventListener(DataEvent.UPLOAD_COMPLETE_DATA, handleUploadComplete);
        }

        _fileRef.browse(getFilters());
    }

    protected function handleFileSelected (event :Event) :void
    {
        var req :URLRequest = new URLRequest(_serverURL + "echouploadsvc");
        req.method = URLRequestMethod.POST;
        trace("Uploading to " + _serverURL + "echouploadsvc:  " + _fileRef.name);

        showProgress(_fileRef);
        _fileRef.upload(req);
    }

    protected function showProgress (source :Object = null) :void
    {
        _progress.source = source;
        var show :Boolean = (source != null);
        _progress.visible = show;
        _progress.includeInLayout = show;
        if (!show) {
            _progress.setProgress(0, 0);
        }
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        trace("Upload error: " + event);
    }

    protected function handleUploadEvent (event :Event) :void
    {
        trace("Upload: " + event);
    }

    protected function handleUploadProgress (event :ProgressEvent) :void
    {
        trace("Progress: " + (event.bytesLoaded / event.bytesTotal).toFixed(3));
    }

    protected function handleUploadComplete (event :DataEvent) :void
    {
        trace("Complete! " + event.data.length + " : " + _fileRef.size);
        var decoder :Base64Decoder = new Base64Decoder();
        decoder.decode(event.data);
        var ba :ByteArray = decoder.toByteArray();

        _bytes = ba;
        _filename = _fileRef.name;
        setImage(_bytes);
        showProgress();
    }

    protected function getFilters () :Array
    {
        var fileType :String = _pack.getFileEntry(_name)["type"];
        var array :Array = [];

        switch (fileType) {
        case "Blob":
            return null;

        case "DisplayObject":
            array.push(new FileFilter("Flash movies", "*.swf"));
            // fall through to Image
        case "Image":
            array.push(new FileFilter("Images", "*.jpg;*.jpeg;*.gif;*.png"));
            return array;

        default:
            throw new Error("Don't understand " + fileType + " files yet.");
        }
    }

    protected var _parent :FileEditor;

    protected var _name :String;

    protected var _pack :EditableDataPack;

    protected var _serverURL :String;

    protected var _filename :String;

    protected var _bytes :ByteArray;

    protected var _fileRef :FileReference;

    protected var _progress :ProgressBar;

    protected var _image :Image;
}
}
