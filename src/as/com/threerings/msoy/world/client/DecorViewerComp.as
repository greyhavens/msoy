//
// $Id$

package com.threerings.msoy.world.client {

import flash.external.ExternalInterface;

import mx.containers.Canvas;
import mx.controls.VSlider;
import mx.controls.Label;

import mx.events.SliderEvent;

public class DecorViewerComp extends Canvas
{
    public static const log :Log = Log.getLog(DecorViewerComp);

    public function DecorViewerComp ()
    {
        if (ExternalInterface.available) {
            try {
                // hook up our ffi
                ExternalInterface.addCallback("updateParameters", updateParameters);
                ExternalInterface.addCallback("updateMedia", updateMedia);
            } catch (err :Error) {
                log.warning("External interface initialization failed: " + err);
            }
        } else {
            log.warning("External interface not available!");
        }
    }

    // inherited from Canvas
    override protected function createChildren () :void
    {
        super.createChildren();

        _results = new Label();
        addChild(_results);

        dlog("Hello, whirled!");

        _horizonSlider = new VSlider();
        _horizonSlider.minimum = 0;
        _horizonSlider.maximum = 1;
        _horizonSlider.liveDragging = true;
        _horizonSlider.addEventListener(SliderEvent.CHANGE, processChange);
        _horizonSlider.x = 10;
        addChild(_horizonSlider);

        _depthSlider = new VSlider();
        _depthSlider.minimum = 0;
        _depthSlider.maximum = 1000;
        _depthSlider.liveDragging = true;
        _depthSlider.addEventListener(SliderEvent.CHANGE, processChange);
        _depthSlider.x = 20;
        addChild(_depthSlider);

        // send an initialization request to GWT
        if (ExternalInterface.available) {
            try {
                ExternalInterface.call("updateDecorInit");
            } catch (e :Error) {
                log.warning("Unable to initialize updates with Javascript: " + e);
            }
        } 
    }

    /**
     * Called whenever any of the UI elements changes.
     */
    protected function processChange (event :SliderEvent) :void
    {
        _horizon = _horizonSlider.value;
        _depth = _depthSlider.value;
        
        sendUpdateToJS();
    }

    /**
     * Called from JavaScript, updates this viewer's internal parameters (width, height, etc.)
     */
    public function updateParameters (
        width :int, height :int, depth :int, horizon :Number, type :int) :void
    {
        _width = width;
        _height = height;
        _depth = depth;
        _type = type;
        _horizon = horizon;

        // update UI
        _horizonSlider.value = _horizon;
        _depthSlider.value = _depth;
        
        dlog("processed update: " + width + "x" + height + "x" + depth +
             ", " + horizon + ", type " + type);
        
    }

    /**
     * Called from JavaScript, updates this viewer's media.
     */
    public function updateMedia (mediaPath :String) :void
    {
        _mediaPath = mediaPath;
    }

    /**
     * Sends the current viewer parameters to JavaScript. Only sends parameters that can change
     * in the viewer (width, height, etc.) - not the media, which can't be modified here.
     */
    public function sendUpdateToJS () :void
    {
        if (ExternalInterface.available) {
            try {
                ExternalInterface.call("updateDecor", _width, _height, _depth, _horizon, _type);
            } catch (e :Error) {
                log.warning("Unable to send update to Javascript: " + e);
            }
        } else {
            log.warning("External interface not available, " +
                        "while trying to send an update to Javascript");
        }
    }

    // TEMP: helper function
    public function dlog (message :String) :void
    {
        if (_testing) {
            _results.text = message;
        }
    }

    protected var _testing :Boolean = true;
    
    protected var _results :Label;
    protected var _horizonSlider :VSlider;
    protected var _depthSlider :VSlider;

    protected var _width :int;
    protected var _height :int;
    protected var _depth :int;
    protected var _horizon :Number;
    protected var _type :int;
    protected var _mediaPath :String;

    [Embed(source="../../../../../../../pages/images/item/detail_preview_bg.png")]
    protected static const BACKGROUND :Class;
}
}

