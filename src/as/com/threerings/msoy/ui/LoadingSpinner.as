//
// $Id$

package com.threerings.msoy.ui {

import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.MovieClip;
import flash.display.Sprite;

import flash.events.Event;

import flash.text.TextField;
import flash.text.TextFormat;

import flash.utils.ByteArray;

// NOTE: minimize any dependancies on non-builtin packages, because this class is
// used by our application preloader.

public class LoadingSpinner extends Sprite
{
    public static const WIDTH :int = 168;
    public static const HEIGHT :int = 116;

    public function LoadingSpinner ()
    {
        // avoiding using MultiLoader to minimize dependancies
        var l :Loader = new Loader();
        l.contentLoaderInfo.addEventListener(Event.COMPLETE, handleComplete);
        l.loadBytes(new SPINNER() as ByteArray);

        _label = new TextField();
        _label.width = WIDTH;
        const tf :TextFormat = new TextFormat();
        tf.font = "_sans";
        tf.align = "center";
        tf.bold = true;
        tf.color = 0xFFFFFF;
        tf.size = 16;
        _label.defaultTextFormat = tf;
        _label.y = HEIGHT;
        addChild(_label);
    }

    /**
     * Set the progress to be displayed on the spinner.
     * Call with no args to set to "indeterminate" mode.
     */
    public function setProgress (partial :Number = NaN, total :Number = NaN) :void
    {
        _progress = Math.round(partial * 100 / total); // evals to NaN if any args are NaN
        updateSpinner();
    }

    protected function handleComplete (event :Event) :void
    {
        _spinner = (event.target as LoaderInfo).loader.content as MovieClip;
        // stop, so that even if we're currently supposed to be on frame 1
        // we don't run past it because updateSpinner() won't gotoAndStop
        // to frame it's already on.
        _spinner.stop();

        addChildAt(_spinner, 0);
        updateSpinner();

        // TODO: do we need to unload?
    }

    protected function updateSpinner () :void
    {
        if (_spinner == null) {
            return;
        }

        if (!isNaN(_progress)) {
            var frame :int = 1 + _progress;
            // avoid re-setting us to the same frame, as that causes the
            // inner swirls to halt (for some reason!)
            if (_spinner.currentFrame != frame) {
                _spinner.gotoAndStop(frame);
            }
            const text :String = _progress.toFixed(0) + "%";
            _label.text = (text == "0.%") ? "0%" : text; // jesus christ

        } else if (_spinner.currentFrame < 102) {
            _spinner.gotoAndPlay(102);
            _label.text = "";
        }
    }

    protected var _progress :Number = NaN;

    protected var _spinner :MovieClip;

    protected var _label :TextField;

    [Embed(source="../../../../../../rsrc/media/loading.swf", mimeType="application/octet-stream")]
    protected static const SPINNER :Class;
}
}
