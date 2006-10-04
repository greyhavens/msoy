package {

import flash.display.Scene;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.TimerEvent;
import flash.utils.describeType;
import flash.utils.Timer;

import mx.core.BitmapAsset;
import mx.core.MovieClipAsset;

public class Bunny extends Sprite
{
    public function Bunny ()
    {
        bunnyMovie = MovieClipAsset(new bunnyAnim());
        var bBox :Shape = new Shape();
        bBox.graphics.lineStyle(1, 0x00FFFF);
        bBox.graphics.drawRect(0, 0, bunnyMovie.width, bunnyMovie.height);
        addChild(bBox);
        idle();
        x = _localX + _offset;
        addChild(bunnyMovie);
        scaleX=2;
        scaleY=2;
    }

    public function walk (deltaX :int) :void
    {
        if (_attacking) {
            return;
        }
        _localX = Math.min(640 - width, Math.max(0, _localX + deltaX));
        if (bunnyMovie.currentFrame != 2) {
            bunnyMovie.gotoAndStop(2);
        }
        var newOrient :int;
        if (deltaX < 0) {
            newOrient = 1;
        } else {
            newOrient = 0;
        }

        if (newOrient != _orient) {
            _orient = newOrient;
            switch (_orient) {
              case 0:
                scaleX = 2;
                _offset = 0;
                break;
              case 1:
                scaleX = -2;
                _offset = width;
                break;
            }
        }
        x = _localX + _offset;
    }

    public function idle () :void
    {
        if (_attacking) {
            return;
        }
        bunnyMovie.gotoAndStop(1);
    }

    public function attack () :void
    {
        if (_attacking) {
            return;
        }
        BunnyKnights.log("ATTACK!!");
        _attacking = true;
        _attackTimer = new Timer(200, 1);
        _attackTimer.addEventListener(TimerEvent.TIMER_COMPLETE, attackOver);
        _attackTimer.start();
        bunnyMovie.gotoAndStop(3);
    }

    public function attackOver (event :TimerEvent) :void
    {
        _attacking = false;
        idle();
    }

    protected var bunnyMovie :MovieClipAsset;

    protected var _orient :int;
    protected var _offset :int;
    protected var _localX :int;
    protected var _attacking :Boolean = false;
    protected var _attackTimer :Timer;

    [Embed(source="rsrc/bunny/bunny_knight_blue.swf#bunny_blue_master")]
    protected var bunnyAnim :Class;
}
}
