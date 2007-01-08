package {

import flash.display.Scene;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.TimerEvent;
import flash.utils.describeType;
import flash.utils.Timer;

import mx.core.BitmapAsset;
import mx.core.MovieClipAsset;

import com.threerings.ezgame.EZGameControl;

public class Bunny extends Sprite
{
    public var ax :int = -1, ay :int = -1;
    public var atile :Tile;

    public function Bunny (board :Board, index :int)
    {
        _board = board;
        if (index == 0) {
            bunnyMovie = MovieClipAsset(new bunnyBlue());
        } else {
            bunnyMovie = MovieClipAsset(new bunnyRed());
        }
        idle();
        addChild(bunnyMovie);
        _moveQueue = new Array();
        _moveStore = new Array();
    }

    public function remoteWalk (delta :int) :void
    {
        if (_attacking) {
            return;
        }
        if (_moveQueue.length == 0) {
            idle();
            return;
        }
        var sum :int = 0;
        var tot :int = 0;
        while (tot < delta && _moveQueue.length > 0) {
            var action :String = String(_moveQueue.shift());
            switch (action) {
              case "w":
              case "c":
                var pop :int = int(_moveQueue.shift());
                tot += Math.abs(pop);
                /*
                if (tot > delta) {
                    var remaining :int = tot - delta;
                    var sign :int = pop / Math.abs(pop);
                    sum += sign * (Math.abs(pop) - remaining);
                    _moveQueue.unshift(remaining * sign);
                    _moveQueue.unshift("w");
                } else {
                    sum += pop;
                }*/
                doAction(action, pop);
                break;
              case "s":
                attack();
                return;
            }
        }
    }

    protected function doAction (action :String, sum :int) :void
    {
        if (sum == 0) {
            return;
        }
        switch (action) {
          case "w":
            walk(sum, false);
            break;
          case "c":
            climb(sum);
            break;
        }
    }

    public function walk (deltaX :int, primary :Boolean = true) :void
    {
        if (_attacking) {
            return;
        }
        if (_climbing) {
            idle();
            return;
        }
        if (bunnyMovie.currentFrame != 2) {
            bunnyMovie.gotoAndStop(2);
        }
        if (deltaX == 0) {
            return;
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
                bunnyMovie.scaleX = 1;
                bunnyMovie.x = 0;
                break;
              case 1:
                bunnyMovie.scaleX = -1;
                bunnyMovie.x = bunnyMovie.width;
                break;
            }
        }
        _board.move(this, deltaX, BWIDTH);
        _moveStore.push("w");
        _moveStore.push(String(deltaX));
    }

    public function climb (deltaY :int) :void
    {
        if (_attacking || deltaY == 0) {
            return;
        }
        if (_board.climb(this, deltaY, BWIDTH)) {
            _climbing = true;
            if (bunnyMovie.currentFrame != 4) {
                bunnyMovie.gotoAndStop(4);
            }
        } else {
            _climbing = false;
            idle();
        }
        _moveStore.push("c");
        _moveStore.push(String(deltaY));
    }

    public function sendStore (gameCtrl :EZGameControl, index :int) :void
    {
        if (_moveStore.length == 0) {
            return;
        }
        var deltas :String = String(_moveStore.shift());
        while (_moveStore.length > 0) {
            deltas += "," + _moveStore.shift();
        }
        gameCtrl.sendMessage("bunny" + index, deltas);
    }

    public function remote (queue :String) :void
    {
        _moveQueue = _moveQueue.concat(queue.split(","));
    }

    public function setCoords (x :int, y :int) :void
    {
        _bY = y;
        this.y = _bY + Tile.TILE_SIZE - height;
        _bX = x;
        this.x = _bX - int((width - BWIDTH)/2);
    }

    public function getBX () :int
    {
        return _bX;
    }

    public function getBY () :int
    {
        return _bY;
    }

    public function idle () :void
    {
        if (_attacking) {
            return;
        }
        if (_climbing) {
            bunnyMovie.gotoAndStop(5);
            return;
        }
        bunnyMovie.gotoAndStop(1);
    }

    public function attack () :void
    {
        if (_attacking || _climbing) {
            return;
        }
        _attacking = true;
        _attackTimer = new Timer(200, 1);
        _attackTimer.addEventListener(TimerEvent.TIMER_COMPLETE, attackOver);
        _attackTimer.start();
        bunnyMovie.gotoAndStop(3);
        _moveStore.push("s");
    }

    public function attackOver (event :TimerEvent) :void
    {
        _attacking = false;
        idle();
    }

    protected var bunnyMovie :MovieClipAsset;

    protected var _orient :int;
    protected var _offset :int;
    protected var _bX :int, _bY :int;
    protected var _attacking :Boolean = false;
    protected var _attackTimer :Timer;
    protected var _board :Board;
    protected var _moveQueue :Array;
    protected var _moveStore :Array;
    protected var _climbing :Boolean = false;
    protected static const BWIDTH :int = 28;
    

    [Embed(source="rsrc/bunny/bunny_knight_blue.swf#bunny_blue_master")]
    protected var bunnyBlue :Class;

    [Embed(source="rsrc/bunny/bunny_knight_blue.swf#bunny_red_master")]
    protected var bunnyRed :Class;
}
}
