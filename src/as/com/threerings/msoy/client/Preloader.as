//
// $Id$

package com.threerings.msoy.client {

import flash.display.Sprite;
import flash.display.MovieClip;

import flash.events.Event;
import flash.events.ProgressEvent;

// NOTE: minimize dependancies outside of flash.*, since this is our preloader...

import com.threerings.msoy.ui.LoadingSpinner;

import mx.events.FlexEvent;

import mx.preloaders.IPreloaderDisplay

public class Preloader extends Sprite
    implements IPreloaderDisplay
{
    public function Preloader ()
    {
        _spinner = new LoadingSpinner();
        _spinner.setProgress(0, 1);
        addChild(_spinner);
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundAlpha (value :Number) :void
    {
        _bgAlpha = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundAlpha () :Number
    {
        return _bgAlpha;
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundColor (value :uint) :void
    {
        _bgColor = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundColor () :uint
    {
        return _bgColor;
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundImage (value :Object) :void
    {
        _bgImage = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundImage () :Object
    {
        return _bgImage;
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundSize (value :String) :void
    {
        _bgSize = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundSize () :String
    {
        return _bgSize;
    }

    // from IPreloaderDisplay and stupidly so
    public function set stageHeight (value :Number) :void
    {
        _stageH = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get stageHeight () :Number
    {
        return _stageH;
    }

    // from IPreloaderDisplay and stupidly so
    public function set stageWidth (value :Number) :void
    {
        _stageW = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get stageWidth () :Number
    {
        return _stageW;
    }

    // from IPreloaderDisplay
    public function set preloader (value :Sprite) :void
    {
        value.addEventListener(ProgressEvent.PROGRESS, handleProgress);
        value.addEventListener(FlexEvent.INIT_COMPLETE, handleComplete);

        // TODO: remove the following debugging
        var mc :MovieClip = value.root as MovieClip;
        var working :Boolean = (mc.framesLoaded < mc.totalFrames);
        trace("----> Preloader " + (working ? "DID" : "did NOT") + " work.");
    }

    // from IPreloaderDisplay
    public function initialize () :void
    {
        _spinner.x = (_stageW - LoadingSpinner.WIDTH) / 2;
        _spinner.y = (_stageH - LoadingSpinner.HEIGHT) / 2;
    }

    protected function handleProgress (event :ProgressEvent) :void
    {
        _spinner.setProgress(event.bytesLoaded, event.bytesTotal);
    }

    protected function handleComplete (event :Event) :void
    {
        // signal flex to start up our app
        dispatchEvent(new Event(Event.COMPLETE));

        // TODO: actually instantiate MsoyClient here, log us in, only start flex once
        // we're all the way logged in
    }

    protected var _spinner :LoadingSpinner;

    protected var _bgAlpha :Number;
    protected var _bgColor :uint;
    protected var _bgImage :Object;
    protected var _bgSize :String;
    protected var _stageH :Number;
    protected var _stageW :Number;
}
}
