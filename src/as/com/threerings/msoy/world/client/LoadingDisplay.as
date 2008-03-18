//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.LoaderInfo;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;

import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFormatAlign;

import com.threerings.msoy.client.PlaceBox;

public class LoadingDisplay extends Sprite
    implements LoadingWatcher
{
    public function LoadingDisplay (box :PlaceBox)
    {
        _box = box;

        x = 10;
        y = 10;

        _spinner = DisplayObject(new SPINNER());
        _spinner.scaleX = .25;
        _spinner.scaleY = .25;
        addChild(_spinner);
    }

    // from interface LoadingWatcher
    public function watchLoader (info :LoaderInfo, isDecor :Boolean = false) :void
    {
        info.addEventListener(Event.COMPLETE, handleComplete);
        info.addEventListener(IOErrorEvent.IO_ERROR, handleIOError);

        if (isDecor) {
            _decor = info;
            info.addEventListener(ProgressEvent.PROGRESS, handleProgress);
            setProgress(0, 1);
            _spinner.scaleX = 1;
            _spinner.scaleY = 1;

        } else {
            _furniCount++;
        }

        // make sure we're showing
        if (parent == null) {
            _box.addOverlay(this, PlaceBox.LAYER_ROOM_SPINNER);
        }
    }

    protected function unwatchLoader (info :LoaderInfo) :void
    {
        info.removeEventListener(Event.COMPLETE, handleComplete);
        info.removeEventListener(IOErrorEvent.IO_ERROR, handleIOError);

        if (info == _decor) {
            info.removeEventListener(ProgressEvent.PROGRESS, handleProgress);
            removeChild(_progress);
            _progress = null;
            _decor = null;
            _spinner.scaleX = .25;
            _spinner.scaleY = .25;

        } else {
            _furniCount--;
        }

        if (_decor == null && _furniCount == 0 && (parent != null)) {
            _box.removeOverlay(this);
        }
    }

    protected function handleComplete (event :Event) :void
    {
        unwatchLoader(event.target as LoaderInfo);
    }

    protected function handleIOError (event :IOErrorEvent) :void
    {
        unwatchLoader(event.target as LoaderInfo);
    }

    protected function handleProgress (event :ProgressEvent) :void
    {
        setProgress(event.bytesLoaded, event.bytesTotal);
    }

    protected function setProgress (partial :Number, total :Number) :void
    {
        if (_progress == null) {
            var tf :TextFormat = new TextFormat();
            tf.size = 32;
            tf.font = "_sans";
            tf.color = 0xFFFFFF;
            tf.align = TextFormatAlign.CENTER;

            _progress = new TextField();
            _progress.defaultTextFormat = tf;
            _progress.width = 110;
            _progress.height = 50;
            _progress.y = 110;
            addChild(_progress);
        }

        var perc :String = Math.round((partial * 100) / total) + "%";
        _progress.text = perc;
    }

    protected var _box :PlaceBox;

    protected var _furniCount :int;

    protected var _decor :LoaderInfo

    protected var _spinner :DisplayObject;

    protected var _progress :TextField;

    [Embed(source="../../../../../../../rsrc/media/loading.swf")]
    protected static const SPINNER :Class;
}
}
