package com.threerings.msoy {

import flash.display.DisplayObjectContainer;
import flash.display.Loader;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.net.URLRequest;

import flash.util.Timer;

public class TestAVM1
{
    public function TestAVM1 (container :DisplayObjectContainer, url :String)
    {
        _loader = new Loader();
        _loader.load(new URLRequest(url));
        container.addChild(_loader);

        _timer = new Timer(20);
        _timer.addEventListener(TimerEvent.TIMER, tick);
        _timer.start();
    }

    public function tick (event :Event) :void
    {
        if (_dir > 0) {
            if (_loader.x + _loader.width >= 900) {
                _dir *= -1;
                _loader.height = (_loader.height / 2);
            }
        } else {
            if (_loader.x <= 0) {
                _dir *= -1;
                _loader.height *= 2;
            }
        }
        _loader.x += _dir;
    }

    protected var _timer :Timer;
    protected var _loader :Loader;
    protected var _dir :int = 4;
}
}
