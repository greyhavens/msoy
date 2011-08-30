//
// $Id$

package com.threerings.msoy {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Loader;
import flash.display.MovieClip;
import flash.events.Event;
import flash.events.TimerEvent;
import flash.net.URLRequest;
import flash.utils.Timer;

public class TestAVM1
{
    public function TestAVM1 (container :DisplayObjectContainer, url :String)
    {
        var loader :Loader = new Loader();
        _disp = loader;
        loader.load(new URLRequest(url));
        container.stage.addChild(loader);

        if (_timer == null) {
            _timer = new Timer(20);
            _timer.start();
        }
        _timer.addEventListener(TimerEvent.TIMER, tick);

/*
        container.stage.addEventListener(Event.RENDER, render);
        loader.addEventListener(Event.RENDER, render);

        var mc :MovieClip = new MovieClip();
        mc.addEventListener(Event.RENDER, render);
        mc.addEventListener("render", render);
        container.addChild(mc);
        mc.play();
        */
    }

    public function tick (event :Event) :void
    {
        if (_dir > 0) {
            if (_disp.x + _disp.width >= 900) {
                _dir *= -1;
                _disp.height = (_disp.height / 2);
            }
        } else {
            if (_disp.x <= 0) {
                _dir *= -1;
                _disp.height *= 2;
            }
        }
        _disp.x += _dir;
        _disp.rotation += 1;
        //_disp.rotation = Math.random() * 360;
        _disp.alpha = Math.random();

        if (_disp is Loader) {
            var l :Loader = (_disp as Loader);
            if (l.content != null) {
                var doc :DisplayObjectContainer = l.parent;
                doc.removeChild(_disp);
                trace("removed loader");

                _disp = l.content;

                doc.addChild(_disp);
                _disp.addEventListener(Event.RENDER, render);
            }
        }
    }

    public function render (event :Event) :void
    {
        var n :Number = new Date().getTime();
        var diff :Number = n - _lastRender;
        _lastRender = n;
        trace("render: " + diff);
    }

    protected static var _timer :Timer;

    protected var _lastRender :Number;
    protected var _disp :DisplayObject;
    protected var _dir :int = 4;
}
}
