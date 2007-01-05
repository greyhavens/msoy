package {

import flash.display.GradientType;
import flash.display.Shape;

import flash.geom.Matrix;

import com.threerings.ezgame.EZGameControl;

public class Bricks extends Shape
{
    /** The number of layers of bricks. */
    public static const LAYERS :int = 10;
    
    public function Bricks (gameCtrl :EZGameControl, board :Board)
    {
        _gameCtrl = gameCtrl;
        _board = board;
        
        // center the bricks on the board
        _columns = int(_board.width / BRICK_WIDTH);
        x = (_board.width - _columns * BRICK_WIDTH) / 2;
        y = (_board.height - LAYERS * BRICK_HEIGHT) / 2;
        
        // initialize the collision times and draw the bricks
        _ctimes = new Array();
        for (var yy :int = 0; yy < LAYERS; yy++) {
            var trow :Array = new Array();
            for (var xx :int = 0; xx < _columns; xx++) {
                trow.push(int.MAX_VALUE);
                drawBrick(xx, yy, true);
            }
            _ctimes.push(trow);
        }
    }
    
    public function get columns () :int
    {
        return _columns;
    }
    
    public function intersect (
        px :Number, py :Number, dx :Number, dy :Number, speed: Number,
        time :int) :Object
    {
        var coll :Object = {dist: Number.MAX_VALUE};
        
        // first check against the verticals
        var t :Number, cx :Number, cy :Number,
            ix :int, iy :int, ctime :int;
        if (dx != 0) {
            // step through each vertical line along the direction of
            // motion, starting with the closest and ending at the furthest
            var bx :Number = (px - x) / BRICK_WIDTH,
                xxstart :int = (dx < 0) ?
                    clamp(Math.floor(bx), 0, _columns) :
                    clamp(Math.ceil(bx), 0, _columns),
                xxmax :int = (dx < 0) ? 0 : _columns,
                xxdir :int = (dx < 0) ? -1 : +1;
            for (var xx :int = xxstart; xx != xxmax; xx += xxdir) {
                cx = x + xx * BRICK_WIDTH - Ball.RADIUS * xxdir;
                t = (cx - px) / dx;
                if (t < 0 || t > coll.dist) {
                    continue;
                }
                cy = py + t * dy;
                ctime = time + t / speed;
                ix = xx - (dx < 0 ? 1 : 0);
                iy = Math.round((cy - y) / BRICK_HEIGHT);
                
                // check for bricks at the two closest spots (the
                // collision regions overlap because of the ball-sized
                // borders) that will exist when the ball reaches the
                // intersection point
                if (cy < y + iy * BRICK_HEIGHT + Ball.RADIUS &&
                    iy > 0 && iy <= LAYERS &&
                    _ctimes[iy - 1][ix] > ctime) {
                    coll = {x: cx, y: cy, dx: -dx, dy: dy, dist: t,
                        bx: ix, by: iy - 1, ctime: ctime};
                        
                } else if (cy > y + iy * BRICK_HEIGHT - Ball.RADIUS &&
                    iy >= 0 && iy < LAYERS &&
                    _ctimes[iy][ix] > ctime) {
                    coll = {x: cx, y: cy, dx: -dx, dy: dy, dist: t,
                        bx: ix, by: iy, ctime: ctime};
                }
            }
        }
        
        // then, the horizontals
        if (dy != 0) {
            var by :Number = (py - y) / BRICK_HEIGHT,
                yystart :int = (dy < 0) ?
                    clamp(Math.floor(by), 0, LAYERS) :
                    clamp(Math.ceil(by), 0, LAYERS),
                yymax :int = (dy < 0) ? 0 : LAYERS,
                yydir :int = (dy < 0) ? -1 : +1;
            for (var yy :int = yystart; yy != yymax; yy += yydir) {
                cy = y + (yy * BRICK_HEIGHT) - (Ball.RADIUS * yydir);
                t = (cy - py) / dy;
                if (t < 0 || t > coll.dist) {
                    continue;
                }
                cx = px + t * dx;
                ctime = time + t / speed;
                iy = yy - (dy < 0 ? 1 : 0);
                ix = Math.round((cx - x) / BRICK_WIDTH);
                if (cx < x + ix * BRICK_WIDTH + Ball.RADIUS &&
                    ix > 0 && ix <= _columns &&
                    _ctimes[iy][ix - 1] > ctime) {
                    coll = {x: cx, y: cy, dx: dx, dy: -dy, dist: t,
                        bx: ix - 1, by: iy, ctime: ctime};
                        
                } else if (cx > x + ix * BRICK_WIDTH - Ball.RADIUS &&
                    ix >= 0 && ix < _columns &&
                    _ctimes[iy][ix] > ctime) {
                    coll = {x: cx, y: cy, dx: dx, dy: -dy, dist: t,
                        bx: ix, by: iy, ctime: ctime};
                }
            }
        }
        
        // update the collision time if there was one
        if (coll.dist < Number.MAX_VALUE) {
            willBeHit(coll.bx, coll.by, coll.ctime);
            return coll;
        } 
        return null;
    }
    
    public function willBeHit (bx :int, by :int, time :int) :void
    {
        _ctimes[by][bx] = Math.min(_ctimes[by][bx], time);
    }
    
    public function wasHit (bx :int, by :int) :void
    {
        drawBrick(bx, by, false);
    }
    
    protected function drawBrick (bx :int, by :int, occupied :Boolean) :void
    {
        graphics.lineStyle(1, Board.BACKGROUND_COLOR);
        if (occupied) {
            var gmat :Matrix = new Matrix();
            gmat.createGradientBox(_columns * BRICK_WIDTH * 1.125,
                LAYERS * BRICK_HEIGHT * 1.125, Math.PI/4);
            graphics.beginGradientFill(GradientType.LINEAR, BRICK_COLORS,
                BRICK_ALPHAS, BRICK_RATIOS, gmat);
        } else {
            graphics.beginFill(Board.BACKGROUND_COLOR);
        }
        graphics.drawRect(bx * BRICK_WIDTH, by * BRICK_HEIGHT,
            BRICK_WIDTH, BRICK_HEIGHT);
    }
    
    protected function clamp (v :Number, min :Number, max :Number) :Number
    {
        return Math.min(Math.max(v, min), max);
    }
    
    protected var _gameCtrl :EZGameControl;
    protected var _board :Board;
    
    /** The number of columns of bricks. */
    protected var _columns :int;
    
    /** Contains the brick collision times. */
    protected var _ctimes :Array;
    
    /** The dimensions of the bricks. */
    protected static const BRICK_WIDTH :int = 40, BRICK_HEIGHT :int = 20;
    
    /** The rainbow colors of the bricks. */
    protected static const BRICK_COLORS :Array =
        [0xFF0000, 0xFFA000, 0xFFFF00, 0x00FF00, 0x0000FF, 0x4B0082, 0x8B00FF];
        
    protected static const BRICK_ALPHAS :Array =
        [1, 1, 1, 1, 1, 1, 1];
    
    protected static const BRICK_RATIOS :Array =
        [0, 42, 84, 126, 168, 210, 252];
}
}
