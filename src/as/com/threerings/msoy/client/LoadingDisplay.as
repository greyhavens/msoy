//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.LoaderInfo;
import flash.display.MovieClip;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.ErrorEvent;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;

import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFormatAlign;

import com.threerings.util.MultiLoader;

import com.threerings.msoy.client.PlaceBox;

public class LoadingDisplay extends Sprite
    implements LoadingWatcher
{
    public function LoadingDisplay (box :PlaceBox)
    {
        _box = box;

        x = 10;
        y = 10;

        MultiLoader.getContents(SPINNER, gotSpinner);
    }

    // from interface LoadingWatcher
    public function watchLoader (info :LoaderInfo, isPrimaryForPlace :Boolean = false) :void
    {
        info.addEventListener(Event.COMPLETE, handleComplete);
        info.addEventListener(Event.UNLOAD, handleComplete);
        info.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        info.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);

        if (isPrimaryForPlace) {
            _primary = info;
            info.addEventListener(ProgressEvent.PROGRESS, handleProgress);
            setProgress(0, 1);

        } else {
            _secondaryCount++;
        }

        updateSpinner();

        // make sure we're showing
        if (parent == null) {
            _box.addOverlay(this, PlaceBox.LAYER_ROOM_SPINNER);
        }
    }

    protected function unwatchLoader (info :LoaderInfo) :void
    {
        info.removeEventListener(Event.COMPLETE, handleComplete);
        info.removeEventListener(Event.UNLOAD, handleComplete);
        info.removeEventListener(IOErrorEvent.IO_ERROR, handleError);
        info.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);

        if (info == _primary) {
            info.removeEventListener(ProgressEvent.PROGRESS, handleProgress);
            _primary = null;

        } else {
            _secondaryCount--;
        }

        updateSpinner();

        if (_primary == null && _secondaryCount == 0 && (parent != null)) {
            _box.removeOverlay(this);
        }
    }

    protected function handleComplete (event :Event) :void
    {
        unwatchLoader(event.target as LoaderInfo);
    }

    protected function handleError (event :ErrorEvent) :void
    {
        unwatchLoader(event.target as LoaderInfo);
    }

    protected function handleProgress (event :ProgressEvent) :void
    {
        setProgress(event.bytesLoaded, event.bytesTotal);
    }

    protected function setProgress (partial :Number, total :Number) :void
    {
        _progress = Math.round((partial * 100) / total);
        updateSpinner();
    }

    protected function gotSpinner (clip :MovieClip) :void
    {
        _spinner = clip;
        addChild(_spinner);
        updateSpinner();
    }

    protected function updateSpinner () :void
    {
        if (_spinner == null) {
            return;
        }

        if (_primary != null) {
            _spinner.gotoAndStop(Math.max(1, _progress));
            _spinner.scaleX = 1;
            _spinner.scaleY = 1;

        } else {
            _spinner.gotoAndStop(100);
            _spinner.scaleX = .4;
            _spinner.scaleY = .4;
        }
    }

    protected var _box :PlaceBox;

    protected var _primary :LoaderInfo

    protected var _secondaryCount :int;

    protected var _spinner :MovieClip;

    protected var _progress :int;

    [Embed(source="../../../../../../rsrc/media/loading.swf", mimeType="application/octet-stream")]
    protected static const SPINNER :Class;
}
}
