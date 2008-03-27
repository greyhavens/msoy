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

import mx.events.ResizeEvent;

import com.threerings.msoy.client.PlaceBox;

import com.threerings.msoy.ui.LoadingSpinner;

public class PlaceLoadingDisplay extends Sprite
    implements LoadingWatcher
{
    public function PlaceLoadingDisplay (box :PlaceBox)
    {
        _box = box;
        _box.addEventListener(ResizeEvent.RESIZE, handleBoxResized);
        addChild(_spinner = new LoadingSpinner());
    }

    protected function handleBoxResized (event :ResizeEvent) :void
    {
        if (_spinner.scaleX == 1) {
            x = (_box.width - LoadingSpinner.WIDTH) / 2;
            y = (_box.height - LoadingSpinner.HEIGHT) / 2;
        }
    }

    // from interface LoadingWatcher
    public function watchLoader (info :LoaderInfo, isPrimaryForPlace :Boolean = false) :void
    {
        info.addEventListener(Event.COMPLETE, handleComplete);
        info.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        info.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);

        if (isPrimaryForPlace) {
            _primary = info;
            info.addEventListener(ProgressEvent.PROGRESS, handleProgress);

            x = (_box.width - LoadingSpinner.WIDTH) / 2;
            y = (_box.height - LoadingSpinner.HEIGHT) / 2;

            _spinner.setProgress(0, 1);
            _spinner.scaleX = 1;
            _spinner.scaleY = 1;

        } else {
            _secondaryCount++;
            _spinner.setProgress();

            x = 20;
            y = 20;

            _spinner.scaleX = .4;
            _spinner.scaleY = .4;
        }

        // make sure we're showing
        if (parent == null) {
            _box.addOverlay(this, PlaceBox.LAYER_ROOM_SPINNER);
        }
    }

    protected function unwatchLoader (info :LoaderInfo) :void
    {
        info.removeEventListener(Event.COMPLETE, handleComplete);
        info.removeEventListener(IOErrorEvent.IO_ERROR, handleError);
        info.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);

        if (info == _primary) {
            info.removeEventListener(ProgressEvent.PROGRESS, handleProgress);
            _primary = null;

        } else {
            _secondaryCount--;
        }

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
        _spinner.setProgress(event.bytesLoaded, event.bytesTotal);
    }

    protected var _box :PlaceBox;

    protected var _primary :LoaderInfo

    protected var _secondaryCount :int;

    protected var _spinner :LoadingSpinner;
}
}
