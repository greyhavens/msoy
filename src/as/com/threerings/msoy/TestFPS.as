package com.threerings.msoy {

import flash.display.DisplayObjectContainer;
import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TimerEvent;

import flash.net.URLRequest;

import flash.system.Security;

import flash.utils.Timer;

import mx.controls.HSlider;
import mx.controls.Label;

import mx.containers.Box;

import mx.core.UIComponent;

import mx.effects.Glow;
import mx.effects.Move;
import mx.effects.Parallel;
import mx.effects.Rotate;
import mx.effects.Sequence;
import mx.effects.Zoom;

import mx.events.EffectEvent;
import mx.events.SliderEvent;

import com.threerings.msoy.data.MediaData;
import com.threerings.msoy.ui.ScreenMedia;

public class TestFPS
{
    public function TestFPS (
        container :DisplayObjectContainer, fpsLabel :Label = null)
    {
//        Security.allowDomain("*");

        ////_container = container;
        var host :UIComponent = new UIComponent();
        host.width = 600;
        host.height = 400;
        container.addChild(host);
        _container = host;

//        host.stage..addEventListener(Event.RENDER, willRender);

        // set up the label
        _fpsLabel = fpsLabel;

        // set up the slider
        var slider :HSlider = _slider = new HSlider();
        slider.minimum = 0;
        slider.maximum = 100;
        slider.snapInterval = 1;
        slider.liveDragging = true;
        slider.addEventListener(SliderEvent.CHANGE, sliderChanged);
        slider.x = 0;
        slider.y = _container.height - slider.height;
        slider.width = _container.width;
        _container.addChild(slider);

        // set up the timer for every ms, which I believe will cause it
        // to fire at most once per frame
        //_timer = new Timer(1);
        //_timer.start();
        //_timer.addEventListener(TimerEvent.TIMER, tick);

        container.stage.addEventListener(Event.ENTER_FRAME, tick);

        _lastTick = flash.utils.getTimer();
    }

    /**
     * Callback when slider value changed.
     */
    protected function sliderChanged (event :SliderEvent) :void
    {
        var value :Number = _slider.value; //event.value;

        // remove any display objects over the limit
        while (value < _media.length) {
            var disp :DisplayObject = (_media.pop() as DisplayObject);
            _container.removeChild(disp);
        }

        // add any new media necessary
        while (value > _media.length) {

            var pick :int = int(Math.random() * MediaData.getTestCount());
            var desc :MediaData = new MediaData(pick);
            //if (url.indexOf("tube") != -1) {
            //    desc = new MediaData(url, 100, 100);
            //}
            var screenMedia :ScreenMedia = new ScreenMedia(desc);
            screenMedia.x = Math.random() * _container.width;
            screenMedia.y = Math.random() * _container.height;
            screenMedia.rotation = Math.random() * 360;

            _container.addChildAt(screenMedia, 0);
            _media.push(screenMedia);

            if (_media.length == 1) {
                doNextMove();
            }
        }
    }

    /** Useful for debugging. */
    protected function effectEnd (event :EffectEvent) :void
    {
        trace("Effect ended: " + event.effectInstance + " : " +
            event.target + " : " + event.currentTarget);
    }

    /** Useful for debugging. */
    protected function effectStart (event :EffectEvent) :void
    {
        trace("Effect start: " + event.effectInstance + " : " +
            event.target + " : " + event.currentTarget);
    }

    /**
     * Start the next media component randomly moving.
     */
    protected function doNextMove () :void
    {
        if (_media.length == 0) {
            return;
        }

        // first pick a random UIComponent to move
        var index :int = Math.floor(Math.random() * _media.length);
        var comp :UIComponent = (_media[index] as UIComponent);

        var move :Move = new Move(comp);
        var theta :Number = Math.random() * Math.PI * 2;
        move.xBy = 100 * Math.sin(theta);
        move.yBy = 100 * Math.cos(theta);
        move.duration = 1000;

        var rot :Rotate = new Rotate(comp);
        rot.duration = 1000;
        rot.angleFrom = comp.rotation;
        rot.angleTo = comp.rotation + (90 * ((Math.random() < .5) ? 1 : -1));
        //rot.originX = comp.width / 2;
        //rot.originY = comp.height / 2;

        var seq :Sequence = new Sequence(comp);
        var zoomIn :Zoom = new Zoom(comp);
        zoomIn.zoomHeightTo = 2;
        zoomIn.zoomWidthTo = 2;
        zoomIn.duration = 500;

        var zoomOut :Zoom = new Zoom(comp);
        zoomOut.zoomHeightTo = 1;
        zoomOut.zoomWidthTo = 1;
        zoomOut.duration = 500;

        seq.addChild(zoomIn);
        seq.addChild(zoomOut);

        var allMoves :Parallel = new Parallel(comp);
        allMoves.addChild(move);
        allMoves.addChild(rot);
        allMoves.addChild(seq);

        /*
        trace(
            "allMoves props: " + allMoves.getAffectedProperties());
        */
        allMoves.addEventListener(EffectEvent.EFFECT_END, moveDidEnd);
        allMoves.play();
    }

    protected function moveDidEnd (event :EffectEvent) :void
    {
        doNextMove();
    }

    /**
     * Callback called when the timer fires.
     */
    protected function tick (event :Event) :void
    {
        for each (var disp :DisplayObject in _media) {
//            if (Math.random() > .25) {
//                continue;
//            }
//            var xdir :int = (Math.random() * 3) - 1;
//            var ydir :int = (Math.random() * 3) - 1;
//            disp.x += xdir;
//            disp.y += ydir;
            disp.rotation += 1;
        }

        if (_fpsLabel == null) {
            return;
        }

        var curTime :uint = flash.utils.getTimer();
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
        trace("willRender(" + event + ")");
    }

    /* The container to which we add our glorious bits. */
    protected var _container :DisplayObjectContainer;

    /** Our timer that fires as often as possible. */
    protected var _timer :Timer;

    /** The ms mark of the last tick. */
    protected var _lastTick :uint;

    protected var _slider :HSlider;

    /** The set of media we're currently rotating. */
    protected var _media :Array = new Array();

    /** The label in which we display the current average frames per second. */
    protected var _fpsLabel :Label;

    /** The time per tick for the last 24 ticks. */
    protected var _tickTimes :Array = new Array();
}
}
