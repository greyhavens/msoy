//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.ErrorEvent;
import flash.events.HTTPStatusEvent;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;
import flash.events.TextEvent;

import flash.net.URLLoader;
import flash.net.URLLoaderDataFormat;
import flash.net.URLRequest;

import flash.utils.ByteArray;

import mx.controls.ButtonBar;
import mx.controls.Label;
import mx.controls.ProgressBar;
import mx.controls.TextArea;
import mx.controls.TextInput;

import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.core.Application;

import mx.events.FlexEvent;
import mx.events.ValidationResultEvent;

import mx.managers.PopUpManager;

import mx.validators.RegExpValidator;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;

public class URLFileChooser extends TitleWindow
{
    public function URLFileChooser ()
    {
        title = "Get from URL";

        _loader = new URLLoader();
        _loader.dataFormat = URLLoaderDataFormat.BINARY;
        _loader.addEventListener(HTTPStatusEvent.HTTP_STATUS, handleDownloadStatus);
        _loader.addEventListener(IOErrorEvent.IO_ERROR, handleDownloadError);
        _loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleDownloadError);
        _loader.addEventListener(Event.COMPLETE, handleDownloadComplete);

        var box :VBox = new VBox();
        addChild(box);

        var lbl :Label = new Label();
        lbl.text = "Enter the URL of the file:";
        box.addChild(lbl);

        _entry = new TextInput();
        _entry.text = "http://"; // start 'em off right
        _entry.addEventListener(FlexEvent.ENTER, handleEntryEnter);
        _entry.percentWidth = 100;
        _entry.minWidth = 300;
        box.addChild(_entry);

        _status = new TextArea();
        _status.percentWidth = 100;
        _status.editable = false;
        _status.setStyle("borderStyle", "none");
        box.addChild(_status);
        
        _progress = new ProgressBar();
        _progress.percentWidth = 100;
        _progress.visible = false;
        _progress.includeInLayout = false;
        _progress.source = _loader;
        box.addChild(_progress);

        var bar :ButtonBar = new ButtonBar();
        bar.addChild(_ok = new CommandButton("OK", startDownload));
        _ok.enabled = false;
        bar.addChild(new CommandButton("Cancel", close));
        box.addChild(bar);

        _validator = new RegExpValidator();
        _validator.expression = URL_REGEXP;
        _validator.flags = URL_FLAGS;
        _validator.source = _entry;
        _validator.property = "text";
        _validator.addEventListener(ValidationResultEvent.VALID, checkValid);
        _validator.addEventListener(ValidationResultEvent.INVALID, checkValid);
        _validator.triggerEvent = Event.CHANGE; //TextEvent.TEXT_INPUT;
        _validator.trigger = _entry;

        PopUpManager.addPopUp(this, Application(Application.application), true);
        PopUpManager.centerPopUp(this);
    }

    protected function handleEntryEnter (event :FlexEvent) :void
    {
        _validator.validate();
        if (_ok.enabled) {
            startDownload();
        }
    }

    protected function checkValid (event :ValidationResultEvent) :void
    {
        _ok.enabled = (event.type == ValidationResultEvent.VALID);
    }

    protected function startDownload () :void
    {
        _ok.enabled = false;
        _entry.enabled = false;
        _progress.includeInLayout = true;
        _progress.visible = true;

        _loader.load(new URLRequest(_entry.text));
    }

    protected function handleDownloadStatus (event :HTTPStatusEvent) :void
    {
        _status.text = "HTTP Status: " + event.status;

        // I'm pretty sure certain URLs will give a status and that's it, so we
        // need to be prepared to stop the download here.
        //downloadStopped();
    }

    protected function handleDownloadError (event :ErrorEvent) :void
    {
        _status.text = "Error downloading: " + event.text;
        downloadStopped();
    }

    protected function downloadStopped () :void
    {
        _entry.enabled = true;
        _progress.includeInLayout = false;
        _progress.visible = false;
    }

    protected function handleDownloadComplete (event :Event) :void
    {
        close([ makeFilename(_entry.text), _loader.data ]);
    }

    protected function makeFilename (url :String) :String
    {
        var lastSlash :int = url.lastIndexOf("/");
        if (lastSlash == -1) {
            return url;
        }
        return url.substr(lastSlash + 1);
    }

    protected function close (returnValue :Object = null) :void
    {
        try {
            _loader.close();
        } catch (err :Error) {
            // ignore
        }

        PopUpManager.removePopUp(this);
        dispatchEvent(new ValueEvent(Event.COMPLETE, returnValue));
    }

    protected var _loader :URLLoader;

    protected var _entry :TextInput;

    protected var _ok :CommandButton;

    protected var _status :TextArea;

    protected var _progress :ProgressBar;

    protected var _validator :RegExpValidator;

    protected static const URL_REGEXP :String = "(http|https|ftp)://\\S+";
    
    protected static const URL_FLAGS :String = "i";
}
}
