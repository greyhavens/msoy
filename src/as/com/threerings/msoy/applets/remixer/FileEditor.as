//
// $Id$

package com.threerings.msoy.applets.remixer {

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

import mx.controls.Label;

import com.threerings.flex.CommandButton;

import com.whirled.remix.data.EditableDataPack;

import com.threerings.msoy.utils.Base64Decoder;

public class FileEditor extends FieldEditor
{
    public function FileEditor (pack :EditableDataPack, name :String, serverURL :String)
    {
        var entry :Object = pack.getFileEntry(name);
        super(pack, name, entry);
        _serverURL = serverURL;

        addUsedCheckBox(entry);

        var lbl :Label = new Label();
        lbl.text = entry.value as String;
        addComp(lbl);

        // TODO, this'll change
        var change :CommandButton = new CommandButton("View/Change", chooseFile, entry.type);
        _component = change;
        addComp(change);
        addDescriptionLabel(entry);
    }

    // Necessary?
    protected function setupUnknown (entry :Object) :void
    {
        var lbl :Label = new Label();
        lbl.text = "Unknown entry of type '" + entry.type + "'.";

        addComp(lbl, 3);
    }

    override protected function updateEntry () :void
    {
        // TODO
        if (!_used.selected) {
            _pack.replaceFile(_name, null, null);
            setChanged();
        }
    }

    protected function chooseFile (fileType :String) :void
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

        _fileRef.browse(getFilters(fileType));
    }

    protected function handleFileSelected (event :Event) :void
    {

        var req :URLRequest = new URLRequest(_serverURL + "echouploadsvc");
        req.method = URLRequestMethod.POST;
        trace("Uploading to " + _serverURL + "echouploadsvc:  " + _fileRef.name);

        _fileRef.upload(req);
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
        trace("Complete! " + event.data);
        var decoder :Base64Decoder = new Base64Decoder();
        decoder.decode(event.data);
        var ba :ByteArray = decoder.toByteArray();

        _pack.replaceFile(_name, _fileRef.name, ba);
        setChanged();
    }

    protected function getFilters (fileType :String) :Array
    {
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

    protected var _fileRef :FileReference;

    protected var _bytes :ByteArray;

    protected var _serverURL :String;
}
}
