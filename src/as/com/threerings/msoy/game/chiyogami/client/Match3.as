package com.threerings.msoy.game.chiyogami.client {

import flash.display.GradientType;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Matrix;
import flash.geom.Point;

import flash.utils.getTimer; // function import

[SWF(width="450", height="100")]
public class Match3 extends Sprite
{
    /** The dimensions of the game. */
    public static const WIDTH :int = 450;
    public static const HEIGHT :int = 100;

    /** The size of blocks. */
    public static const BLOCK_WIDTH :int = 25;
    public static const BLOCK_HEIGHT :int = 20;

    /** The number of columns and rows that we get from the above two sizes. */
    public static const COLS :int = (WIDTH / BLOCK_WIDTH);
    public static const ROWS :int = (HEIGHT / BLOCK_HEIGHT);

    public function Match3 ()
    {
        // create a background sprite that will also receive mouse events
        // even when no Block is in that place
        var bkg :Sprite = new Sprite();
        var gradix :Matrix = new Matrix();
        gradix.createGradientBox(WIDTH, HEIGHT);
        with (bkg.graphics) {
            beginGradientFill(GradientType.LINEAR, [ 0x333333, 0xCCCCCC ],
                [ 1, 1 ], [ 0, 255 ], gradix);
            drawRect(0, 0, WIDTH, HEIGHT);
            endFill();
        }
        addChild(bkg);

        // the _board sprite contains pieces, so that all pieces are under the cursor
        addChild(_board);

        // create the cursor
        _cursor = new Cursor();
        _cursor.x = 0;
        _cursor.y = 0;
        addChild(_cursor);

        // set up the board: create off-screen black blocks that will act
        // as "stoppers" for falling blocks.
        for (var yy :int = 0; yy < ROWS; yy++) {
            var xx :int = (yy % 2 == 0) ? -1 : COLS;
            var block :Block = new Block(0x000000, xx, yy, _blocks);
            // Note: no need to add it to the display, it's just needed
            // logically.
        }

        // configure a mask so that blocks coming in from the edges don't
        // paint-out
        var masker :Shape = new Shape();
        with (masker.graphics) {
            beginFill(0xFFFFFF);
            drawRect(0, 0, WIDTH, HEIGHT);
            endFill();
        }
        this.mask = masker;
        addChild(masker);

        addEventListener(MouseEvent.MOUSE_MOVE, handleMouseMove);
        addEventListener(MouseEvent.CLICK, handleMouseClick);
        addEventListener(Event.ENTER_FRAME, handleEnterFrame);
    }

    protected function handleEnterFrame (event :Event) :void
    {
        trace("-----------");
        var stamp :Number = getTimer();
        var yy :int;
        var xx :int;
        var dir :int;
        var block :Block;
        var other :Block;

        // first, ensure that there are blocks sitting in the off-screen
        // fall-in positions
        for (yy = 0; yy < ROWS; yy++) {
            dir = (yy % 2 == 0) ? -1 : 1;
            xx = (dir == 1) ? 0 : COLS - 1;

            if ((null == _blocks.get(xx, yy)) && (null == _blocks.get(xx - dir, yy))) {
                //trace("Adding block at " + (xx - dir) + ", " + yy);
                block = new Block(pickBlockColor(), xx - dir, yy, _blocks);
                _board.addChild(block);
            }
        }

        // then, look for blocks that are not moving with no adjacent
        // block in the fall direction
        for (yy = 0; yy < ROWS; yy++) {
            // figure out the direction of falling on this row
            dir = (yy % 2 == 0) ? -1 : 1;

            // preload the first 'other' block
            block = _blocks.get((dir == 1) ? COLS : -1, yy);
            for (xx = (dir == 1) ? COLS - 1: 0; (dir == 1) ? (xx >= -1) : (xx <= COLS); xx -= dir) {

                // the previous block becomes 'other'
                other = block;
                block = _blocks.get(xx, yy);
                if (block != null && block.isStopped() &&
                        ((other == null) || other.isFalling())) {
                    block.setFalling(dir, stamp);
                }
            }
        }

        var allBlocks :Array = _blocks.getAll();
        // ok, then update each block
        for each (block in allBlocks) {
            if (block.isSwapping()) {
                block.update(stamp);
            }
        }

        for each (block in allBlocks) {
            if (block.isFalling()) {
                block.update(stamp);
            }
        }

        // TEMP: look for lost blocks
        allBlocks = _blocks.getAll();
        for (var ii :int = _board.numChildren - 1; ii >= 0; ii--) {
            block = (_board.getChildAt(ii) as Block);
            if (allBlocks.indexOf(block) == -1) {
                trace("Oh lordy! We have an orphan: " + block);
            }
        }
    }

    protected function handleMouseMove (event :MouseEvent) :void
    {
        var p :Point = globalToLocal(new Point(event.stageX, event.stageY));
        var xx :int = Math.floor(p.x / BLOCK_WIDTH);
        var yy :int = Math.min(ROWS - 2, Math.max(0, 
            Math.floor((p.y - (BLOCK_HEIGHT/2)) / BLOCK_HEIGHT)));

        _cursor.x = xx * BLOCK_WIDTH;
        _cursor.y = yy * BLOCK_HEIGHT;

        event.updateAfterEvent(); // for the snappyness
    }

    protected function handleMouseClick (event :MouseEvent) :void
    {
        var lx :int = _cursor.x / BLOCK_WIDTH;
        var ly :int = _cursor.y / BLOCK_HEIGHT;

        var b1 :Block = _blocks.get(lx, ly);
        var b2 :Block = _blocks.get(lx, ly + 1);
        if (b1 == null && b2 == null) {
            // nothing to do, move along
            return;
        }

        var stamp :Number = getTimer();
        for (var yy :int = 0; yy <= 1; yy++) {
            var block :Block = (yy == 0) ? b1 : b2;
            if (block == null) {
                // add a placeholder swap block
                block = new Block(Block.SWAPPER, lx, ly + yy, _blocks);

            } else if (!block.isSwapping()) {
                block.setSwapping((yy == 0) ? 1 : -1, stamp);
                trace("Started swap: " + block);
            }
        }

        event.updateAfterEvent();
    }

    protected function pickBlockColor () :uint
    {
        var pick :int = Math.floor(Math.random() * COLORS.length);
        return uint(COLORS[pick]);
    }

    /** The cursor. */
    protected var _cursor :Cursor;

    protected var _board :Sprite = new Sprite();

    protected var _blocks :BlockMap = new BlockMap();

    /** Block colors. */
    protected static const COLORS :Array = [
        0xFF00EE, // pink
        0xFFFB00, // yellow
        0x00FFF2, // cyan
        0x04FF00, // green
        0x002BFF  // blue
    ];
}
}

import flash.display.Sprite;

import flash.utils.getTimer; // function import

import com.threerings.util.HashMap;

import com.threerings.msoy.game.chiyogami.client.Match3;

class Cursor extends Sprite
{
    public function Cursor ()
    {
        for (var ii :int = 0; ii < 2; ii++) {
            with (graphics) {
                if (ii == 0) {
                    lineStyle(3, 0x000000);
                } else {
                    lineStyle(1, 0xFF0000);
                }

                var y3 :Number = Match3.BLOCK_HEIGHT / 3;
                var h :Number = Match3.BLOCK_HEIGHT * 2;

                moveTo(0, y3);
                lineTo(0, 0);
                lineTo(Match3.BLOCK_WIDTH, 0);
                lineTo(Match3.BLOCK_WIDTH, y3);


                moveTo(0, h - y3);
                lineTo(0, h);
                lineTo(Match3.BLOCK_WIDTH, h);
                lineTo(Match3.BLOCK_WIDTH, h - y3);
            }
        }
    }

}

class BlockMap
{
    public function add (block :Block) :void
    {
        if (undefined !== _map.put(keyFor(block.lx, block.ly), block)) {
            throw new Error("Already a block at (" + block.lx + ", " + block.ly + ")");
        }
    }

    public function get (lx :int, ly :int) :Block
    {
        return (_map.get(keyFor(lx, ly)) as Block);
    }

    public function move (oldx :int, oldy :int, block :Block) :void
    {
        removeTolerant(oldx, oldy, block);
        // and we are allowed to overwrite another block..
        _map.put(keyFor(block.lx, block.ly), block);
    }

    public function remove (block :Block) :void
    {
        if (block !== _map.remove(keyFor(block.lx, block.ly))) {
            throw new Error("Removed block not at (" + block.lx + ", " + block.ly + ")");
        }
    }

    public function removeTolerant (oldx :int, oldy :int, block :Block) :void
    {
        // here we are extremely forgiving- the block may not be in the old loc
        var oldKey :int = keyFor(oldx, oldy);
        if (block === _map.get(oldKey)) {
            _map.remove(oldKey);
        }
    }

    public function getAll () :Array
    {
        return _map.values();
    }

    protected function keyFor (lx :int, ly :int) :int
    {
        // we multiply by 1000 to spread things out, since our
        // x coordinates can vary from -1 to ROWS + 1.
        return ly * 1000 + lx;
    }

    protected var _map :HashMap = new HashMap();
}

class Block extends Sprite
{
    /** The color of the block. */
    public var color :uint;

    /** The block's logical X. */
    public var lx :int;

    /** The block's logical Y. */
    public var ly :int;

    public static const SWAPPER :uint = 0xdeadbe;

    /**
     * Create a block of the specified color.
     */
    public function Block (color :uint, lx :int, ly :int, map :BlockMap)
    {
        this.color = color;
        this.lx = lx;
        this.ly = ly;
        _map = map;

        this.x = lx * Match3.BLOCK_WIDTH;
        this.y = ly * Match3.BLOCK_HEIGHT;
        
        // try adding ourselves to the map
        map.add(this);

        if (color != SWAPPER) {
            with (graphics) {
                beginFill(color);
                drawRect(0, 0, Match3.BLOCK_WIDTH, Match3.BLOCK_HEIGHT);
                endFill();

                lineStyle(1, 0);
                drawRect(0, 0, Match3.BLOCK_WIDTH, Match3.BLOCK_HEIGHT);
            }
        }
    }

    public function isStopped () :Boolean
    {
        return (_movement == NONE);
    }

    public function isFalling () :Boolean
    {
        return (_movement == FALL);
    }

    public function isSwapping () :Boolean
    {
        return (_movement == SWAP);
    }

    /**
     */
    public function setFalling (direction :Number, stamp :Number) :void
    {
        startMove(FALL, direction, stamp);
    }

    public function setSwapping (direction :Number, stamp :Number) :void
    {
        // bump X to its precise value
        x = lx * Match3.BLOCK_WIDTH;

        startMove(SWAP, direction, stamp);
    }

    public function update (stamp :Number) :void
    {
        if (_movement == NONE) {
            return;
        }

        var elapsed :Number = stamp - _moveStamp;

        if (_movement == FALL) {
            // calculate a new X
            var newX :Number = _orig + (GRAVITY * _dir * elapsed * elapsed);

            // check to see if there's a hard limit to where we'll fall
            var other :Block = _map.get(lx + _dir, ly);
            if (other != null) {// && !other.isFalling()) {
                var xlimit :Number = lx * Match3.BLOCK_WIDTH;
                var doStop :Boolean = (_dir == 1) ? (newX >= xlimit) : (newX <= xlimit);
                if (doStop) {
                    newX = xlimit;
                    _movement = NONE;
                }
            }

            // assign the new x coordinate
            x = newX;

            // see if we need to update our logical position
            var newlx :int;
            if (newX < 0) {
                newlx = (newX - Match3.BLOCK_WIDTH/2) / Match3.BLOCK_WIDTH;
            } else {
                newlx = (newX + Match3.BLOCK_WIDTH/2) / Match3.BLOCK_WIDTH;
            }
            if (newlx != lx) {
                if (newlx != (lx + _dir)) {
                    // don't let it move too much in one tick
                    newlx = lx + _dir;
                    x = newlx * Match3.BLOCK_WIDTH;
                    if (_movement == NONE) {
                        throw new Error("ACk! This shouldn't happen");
                    }
                }
                var oldlx :int = lx;
                lx = newlx;
                _map.move(oldlx, ly, this);
            }

        } else if (_movement == SWAP) {
            var newY :Number = _orig + (_dir * SWAP_VELOCITY * elapsed);
            trace("Swap newY: " + newY);
            var ylimit :Number = _orig + (_dir * Match3.BLOCK_HEIGHT);
            if ((_dir == 1) ? (newY >= ylimit) : (newY <= ylimit)) {
                newY = ylimit;
                _movement = NONE; // maybe falling next tick...
            }

            y = newY;

            var newly :int = (newY + Match3.BLOCK_HEIGHT/2 + (_dir * .001)) / Match3.BLOCK_HEIGHT;
            if (newly != ly) {
                var oldly :int = ly;
                ly = newly;
                if (color == SWAPPER) {
                    _map.removeTolerant(lx, oldly, this);
                    // the swapper just goes away
                } else {
                    _map.move(lx, oldly, this);
                }
            }
        }
    }

    override public function toString () :String
    {
        return "Block[x=" + x.toFixed(2) + ", y=" + y.toFixed(2) +
            ", lx=" + lx + ", ly=" + ly + ", movement=" + _movement + "]";
    }

    protected function startMove (movement :int, dir :Number, stamp :Number) :void
    {
        _movement = movement;
        _dir = dir;
        _moveStamp = stamp;
        _orig = (movement == FALL) ? (lx * Match3.BLOCK_WIDTH)
                                   : (ly * Match3.BLOCK_HEIGHT);
    }

    protected var _map :BlockMap;

    /** Movement types. */
    protected static const NONE :int = 0;
    protected static const FALL :int = 1;
    protected static const SWAP :int = 2;

    protected var _movement :int = NONE;

    /** The original x or y, depending on whether we're falling or swapping. */
    protected var _orig :int;

    protected var _dir :Number;

    /** The stamp at which we started moving. */
    protected var _moveStamp :Number;

    protected static const GRAVITY :Number = .0000098;

    protected static const SWAP_VELOCITY :Number = Match3.BLOCK_HEIGHT / 200;
}
