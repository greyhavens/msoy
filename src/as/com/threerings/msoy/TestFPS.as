package com.threerings.msoy {

import flash.display.DisplayObjectContainer;
import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.net.URLRequest;

import flash.util.Timer;

import mx.controls.HSlider;
import mx.controls.Label;

import mx.core.UIComponent;

import mx.events.SliderEvent;

public class TestFPS
{
    public function TestFPS (
        container :DisplayObjectContainer, fpsLabel :Label = null)
    {
        var host :UIComponent = new UIComponent();
        host.width = 600;
        host.height = 400;
        container.addChild(host);
        _container = host;

//        host.stage..addEventListener(Event.RENDER, willRender);

        // set up the label
        _fpsLabel = fpsLabel;

        // set up the slider
        var slider :HSlider = new HSlider();
        slider.minimum = 0;
        slider.maximum = 200;
        slider.snapInterval = 1;
        slider.liveDragging = true;
        slider.addEventListener(SliderEvent.CHANGE, sliderChanged);
        slider.x = 0;
        slider.y = _container.height - slider.height;
        slider.width = _container.width;
        _container.addChild(slider);

        // set up the timer for every ms, which I believe will cause it
        // to fire at most once per frame
        _timer = new Timer(1);
        _timer.start();
        _timer.addEventListener(TimerEvent.TIMER, tick);

        _lastTick = flash.util.getTimer();
    }

    /**
     * Callback when slider value changed.
     */
    protected function sliderChanged (event :SliderEvent) :void
    {
        var value :Number = event.value;

        // remove any display objects over the limit
        while (value < _media.length) {
            var disp :DisplayObject = (_media.pop() as DisplayObject);
            _container.removeChild(disp);
        }

        // add any new media necessary
        while (value > _media.length) {
            var loader :Loader = new Loader();
            //loader.cacheAsBitmap = true;
            var url :String =
                (URLS[Math.floor(Math.random() * URLS.length)] as String);
            loader.load(new URLRequest(url));
            loader.x = Math.random() * _container.width;
            loader.y = Math.random() * _container.height;
            loader.rotation = Math.random() * 360;
            _container.addChildAt(loader, 0);
            _media.push(loader);
        }
    }

    /**
     * Callback called when the timer fires.
     */
    protected function tick (event :Event) :void
    {
        for each (var disp :DisplayObject in _media) {
            if (Math.random() > .25) {
                continue;
            }
            var xdir :int = (Math.random() * 3) - 1;
            var ydir :int = (Math.random() * 3) - 1;
            disp.x += xdir;
            disp.y += ydir;
            disp.rotation += 1;
        }

        if (_fpsLabel == null) {
            return;
        }

        var curTime :uint = flash.util.getTimer();
        _tickTimes.push(curTime - _lastTick);
        _lastTick = curTime;

        if (_tickTimes.length > 24) {
            _tickTimes.shift();
        }

        var total :Number = 0;
        for each (var ms :uint in _tickTimes) {
            total += ms;
        }

        _fpsLabel.text = String((1000 * _tickTimes.length) / total);
    }

    protected function willRender (event :Event) :void
    {
        flash.util.trace("willRender(" + event + ")");
    }

    /* The container to which we add our glorious bits. */
    protected var _container :DisplayObjectContainer;

    /** Our timer that fires as often as possible. */
    protected var _timer :Timer;

    /** The ms mark of the last tick. */
    protected var _lastTick :uint;

    /** The set of media we're currently rotating. */
    protected var _media :Array = new Array();

    /** The label in which we display the current average frames per second. */
    protected var _fpsLabel :Label;

    /** The time per tick for the last 24 ticks. */
    protected var _tickTimes :Array = new Array();

    /** The content we'll swirl around. */
    protected const URLS :Array = [
        "http://bogocorp.com/bogologo.gif",
        "http://www.puzzlepirates.com/images/index/screen2.png",
        "http://www.puzzlepirates.com/images/index/screen3.png",
        "http://www.puzzlepirates.com/images/puzzles/bilge/girl.swf",
        "http://www.puzzlepirates.com/images/puzzles/sword/girl.swf",
        "http://tasman.sea.earth.threerings.net/~ray/AvatarTest.swf"
    ];
}
}
