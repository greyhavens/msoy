//
// $Id#

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Point;

import caurina.transitions.Tweener;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.flash.media.MediaPlayerCodes;
import com.threerings.flash.media.VideoPlayer;

/**
 * The msoy-skinned video display.
 */
// NOTES:
// - Do we want to allow the youtube watermark to be clickable? - NO
public class MsoyVideoDisplay extends Sprite
{
    public static const WIDTH :int = 320;

    public static const HEIGHT :int = 240;

    /**
     * Create.
     */
    public function MsoyVideoDisplay (player :VideoPlayer, commentCallback :Function = null)
    {
        _player = player;
        _player.addEventListener(MediaPlayerCodes.STATE, handlePlayerState);
        _player.addEventListener(MediaPlayerCodes.SIZE, handlePlayerSize);
        _controls = new MediaControls(player, commentCallback);

        addChild(_player.getDisplay());

        addEventListener(Event.ADDED_TO_STAGE, handleAddedToStage);

        configureUI();
    }

    override public function get width () :Number
    {
        return WIDTH;
    }

    override public function get height () :Number
    {
        return HEIGHT;
    }

    /**
     * Stop playing our video.
     */
    public function unload () :void
    {
        _controls.unload();
    }

    protected function configureUI () :void
    {
        // create the mask...
        var masker :Shape = new Shape();
        this.mask = masker;
        addChild(masker);

        var g :Graphics = masker.graphics;
        g.clear();
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 0, WIDTH, HEIGHT);
        g.endFill();

        _hud = new Sprite();
        g = _hud.graphics;
        g.beginFill(0x000000, .85);
        // +5 to give some more room for the bouncy tween
        g.drawRect(0, 0, WIDTH, MediaControls.HEIGHT + 5);
        g.endFill();
        g.lineStyle(1, 0x3db8eb);
        g.moveTo(0, 0);
        g.lineTo(WIDTH, 0);
        _hud.y = HEIGHT + 1; // position it offscreenz
        addChild(_hud);

        _hud.addChild(_controls);
        handleAddedToStage(); // will be ok if we're not..

        addEventListener(MouseEvent.ROLL_OVER, handleMouseRoll);
        addEventListener(MouseEvent.ROLL_OUT, handleMouseRoll);
    }

    protected function handlePlayerState (event :ValueEvent) :void
    {
        _playing = (MediaPlayerCodes.STATE_PLAYING == event.value);
        updateHUD();
    }

    // TODO: Remove this whole thing? Or just the redispatch?
    protected function handlePlayerSize (event :ValueEvent) :void
    {
        const size :Point = Point(event.value);

        const disp :DisplayObject = _player.getDisplay();
        // TODO: siggggghhhhh
//        disp.scaleX = NATIVE_WIDTH / size.x;
//        disp.scaleY = NATIVE_HEIGHT / size.y;
        disp.width = WIDTH;
        disp.height = HEIGHT;

        // and, we redispatch
        dispatchEvent(event);
    }

    protected function handleAddedToStage (event :Event = null) :void
    {
        var mx :Number = this.mouseX;
        var my :Number = this.mouseY;
        _mouseIn = (mx >= 0) && (mx < WIDTH) && (my >= 0) && (my < HEIGHT);
        updateHUD();
    }

    protected function handleMouseRoll (event :MouseEvent) :void
    {
        _mouseIn = (event.type == MouseEvent.ROLL_OVER);
        updateHUD();
    }

    protected function updateHUD () :void
    {
        const show :Boolean = _mouseIn || !_playing || _controls.isDraggingSeek()
        Tweener.addTween(_hud, {
            time: .35,
            y: show ? HEIGHT - MediaControls.HEIGHT : HEIGHT + 1,
            transition: show ? "easeoutback" : "easeinback" });
    }

    protected const log :Log = Log.getLog(this);

    protected var _player :VideoPlayer;

    protected var _controls :MediaControls;

    protected var _hud :Sprite;

    protected var _mouseIn :Boolean;

    protected var _playing :Boolean;
}
}
