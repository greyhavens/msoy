package com.threerings.msoy.game.chiyogami.client {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.text.TextField;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.media.Sound;
import flash.media.SoundChannel;

import flash.utils.getTimer; // function import
import flash.utils.Timer;

import mx.containers.Canvas;

import com.threerings.flash.FPSDisplay;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.item.web.MediaDesc;

public class ChiyogamiPanel extends Canvas
    implements PlaceView
{
    public function ChiyogamiPanel (ctx :WorldContext, ctrl :ChiyogamiController)
    {
        // TODO: Splash screen
    }

    // from PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
    }

    /**
     * Start things a-moving. TODO
     */
    public function startup () :void
    {
        var minigame :KeyJam = new KeyJam(500);
        minigame.y = 500;
        addChild(minigame);

    }
}
}
