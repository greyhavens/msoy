//
// $Id$

package com.threerings.msoy.applets.util {

import flash.events.Event;
import flash.events.ErrorEvent;
import flash.events.HTTPStatusEvent;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;

import flash.net.URLLoader;
import flash.net.URLLoaderDataFormat;
import flash.net.URLRequest;

import flash.utils.ByteArray;

import mx.controls.ProgressBar;
import mx.controls.TextArea;

import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.events.CloseEvent;

import mx.managers.PopUpManager;

import com.threerings.util.ValueEvent;

import com.threerings.flex.FlexUtil;

import com.threerings.msoy.applets.AppletContext;

public class Downloader extends TitleWindow
{
    public function Downloader (ctx :AppletContext)
    {
        title = ctx.APPLET.get("t.downloading");

        _loader = new URLLoader();
        _loader.dataFormat = URLLoaderDataFormat.BINARY;
        _loader.addEventListener(HTTPStatusEvent.HTTP_STATUS, handleDownloadStatus);
        _loader.addEventListener(IOErrorEvent.IO_ERROR, handleDownloadError);
        _loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleDownloadError);
        _loader.addEventListener(Event.COMPLETE, handleDownloadComplete);

        PopUpManager.addPopUp(this, ctx.getApplication(), true);
        PopUpManager.centerPopUp(this);
    }

    public function startDownload (url :String = null, forcedName :String = null) :void
    {
        if (url == null) {
            return;
        }

        _forcedName = forcedName;
        FlexUtil.setVisible(_progress, true);
        _url = url;
        _loader.load(new URLRequest(url));
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var box :VBox = new VBox();
        addChild(box);

        configureUI(box);
    }

    protected function configureUI (box :VBox) :void
    {
        showCloseButton = true;
        addEventListener(CloseEvent.CLOSE, handleClose);

        _status = new TextArea();
        _status.percentWidth = 100;
        _status.editable = false;
        _status.setStyle("borderStyle", "none");
        box.addChild(_status);

        _progress = new ProgressBar();
        _progress.percentWidth = 100;
        FlexUtil.setVisible(_progress, false);
        _progress.source = _loader;
        box.addChild(_progress);
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
        FlexUtil.setVisible(_progress, false);
    }

    protected function handleDownloadComplete (event :Event) :void
    {
        close([ makeFilename(_url), _loader.data ]);
    }

    protected function makeFilename (url :String) :String
    {
        if (_forcedName != null) {
            return _forcedName;
        }

        var lastSlash :int = url.lastIndexOf("/");
        if (lastSlash == -1) {
            return url;
        }
        return url.substr(lastSlash + 1);
    }

    protected function handleClose (event :CloseEvent) :void
    {
        close();
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

    protected var _status :TextArea;

    protected var _progress :ProgressBar;

    protected var _url :String;

    /** If non-null, a forced filename to use with this download. */
    protected var _forcedName :String;
}
}
