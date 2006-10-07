package {

import flash.display.Graphics;
import flash.display.Sprite;
import flash.geom.Matrix;

public class Board
{
    public var bwidth :int, bheight :int;
    public static const BOARD_WIDTH :int = 20;
    public static const BOARD_HEIGHT :int = 15;

    public function Board (bn :BunnyKnights, width :int, height :int)
    {
        _bn = bn;
        bwidth = width;
        bheight = height;
        _shadow = new Array(bwidth, bheight);
        _tiles = new Array();
        _doors = new Array();
        var backLayer :Sprite = _bn.getLayer(Tile.LAYER_BACK);
        backLayer.graphics.beginFill(0x000000);
        backLayer.graphics.drawRect(
                0, 0, bwidth*Tile.TILE_SIZE, bheight * Tile.TILE_SIZE);
        backLayer.graphics.endFill();
    }

    public function addBlocks (
            type :int, x :int, y :int, width :int = 1, height :int = 1) :void
    {
        var baseTile :Tile = Tile.block(0, 0, type);
        for (var yy :int = y; yy < y + height; yy += baseTile.height) {
            for (var xx :int = x; xx < x + width; xx += baseTile.width) {
                addTile(Tile.block(xx, yy, type));
            }
        }
    }

    public function addTiles (
            tile :Function, x :int, y :int, width :int = 1, height :int = 1)
            :void
    {
        var baseTile :Tile = tile(0, 0);
        for (var yy :int = y; yy < y + height; yy += baseTile.height) {
            for (var xx :int = x; xx < x + width; xx += baseTile.width) {
                addTile(tile(xx, yy));
            }
        }
    }

    public function addDoor (tile :Tile) :int
    {
        var doorArray :Array = new Array();
        doorArray.push(tile);
        _doors.push(doorArray);
        addTile(tile);
        return _doors.length - 1;
    }

    public function addSwitch (tile :Tile, idx :int) :void
    {
        var doorArray :Array = _doors[idx] as Array;
        doorArray.push(tile);
        tile.assoc = idx;
        addTile(tile);
    }

    public function addTile (tile :Tile) :void
    {
        if (tile.effect <= Tile.EFFECT_LADDER) {
            var layerSprite :Sprite = _bn.getLayer(tile.layer);
            var matrix :Matrix = new Matrix();
            matrix.translate(tile.x * Tile.TILE_SIZE, tile.y * Tile.TILE_SIZE);
            layerSprite.graphics.beginBitmapFill(tile.getBitmapData(), matrix);
            layerSprite.graphics.drawRect(
                tile.x * Tile.TILE_SIZE, tile.y * Tile.TILE_SIZE,
                tile.width * Tile.TILE_SIZE, tile.height * Tile.TILE_SIZE);
            layerSprite.graphics.endFill();
        } else {
            _tiles.push(tile);
            var tileSprite :Sprite = tile.getSprite();
            tileSprite.x = Tile.TILE_SIZE * tile.x;
            tileSprite.y = Tile.TILE_SIZE * tile.y;
            _bn.addChildToLayer(tileSprite, tile.layer);
        }
        shadowTile(tile);
    }

    public function addBunny (bunny :Bunny, x :int, y :int) :void
    {
        bunny.setCoords(x * Tile.TILE_SIZE, y * Tile.TILE_SIZE);
        _bn.addChildToLayer(bunny, Tile.LAYER_ACTION_FRONT);
    }

    public function tswitch (on :Boolean, x :int, y :int, idx :int) :void
    {
        var doorArr :Array = _doors[idx] as Array;
        var door :Tile = Tile(doorArr[0]);
        var state :int = Tile.STATE_CLOSED;
        if (on) state = Tile.STATE_OPENED;
        var gstate :int = Tile.STATE_CLOSED;
        for (var ii :int = 1; ii < doorArr.length; ii++) {
            var tile :Tile = Tile(doorArr[ii]);
            if (tile.x == x && tile.y == y) {
                tile.setState(state);
                var bunnies :Array = _bn.getBunnies();
                var offset :int;
                if (on) offset = 3;
                for (var jj :int = 0; jj < bunnies.length; jj++) {
                    var bunny :Bunny = Bunny(bunnies[jj]);
                    if (bunny.atile != tile) {
                        continue;
                    }
                    
                    bunny.setCoords(bunny.getBX(), 
                            (tile.y - 1) * Tile.TILE_SIZE + offset);
                }
            }
            if (tile.state == Tile.STATE_OPENED) {
                gstate = Tile.STATE_OPENED;
            }
        }
        door.setState(gstate);
        shadowTile(door);
    }

    public function move (bunny :Bunny, delta :int, width :int) :void
    {
        // don't allow a movement longer than 1 tile until I implment
        // proper collision detection
        delta = Math.max(Math.min(delta, Tile.TILE_SIZE-1), -Tile.TILE_SIZE+1);
        var bx :int = bunny.getBX();
        var by :int = bunny.getBY();
        var tx1 :int = int((bx + delta) / Tile.TILE_SIZE);
        var tx2 :int = int((bx + delta + width) / Tile.TILE_SIZE);
        var ty :int = int(by / Tile.TILE_SIZE);
        if (!canPass(tx1, ty) || !canWalk(tx1, ty + 1)) {
            if (int(bx / Tile.TILE_SIZE) < tx1) {
                bx = tx1 * Tile.TILE_SIZE - width;
            } else {
                bx = (tx1 + 1) * Tile.TILE_SIZE;
            }
        } else if (!canPass(tx2, ty) || !canWalk(tx2, ty + 1)) {
            if (int((bx + width) / Tile.TILE_SIZE) < tx2) {
                bx = tx2 * Tile.TILE_SIZE - width - 1;
            } else {
                bx = tx2 * Tile.TILE_SIZE;
            }
        } else {
            bx += delta;
        }
        var switchWidth :int = Tile.TILE_SIZE * Tile.SWITCH_SIZE;
        var tx :int = int((bx + width/2) / Tile.TILE_SIZE);
        ty = int((by + Tile.TILE_SIZE) / Tile.TILE_SIZE);
        if ((int(_shadow[tx + ty * bwidth]) == Tile.EFFECT_SWITCH) &&
            (int(_shadow[tx - 1 + ty * bwidth]) == Tile.EFFECT_SWITCH)) {
            tx--;
        }
        var doorArr :Array;
        if (bunny.ax > -1 && (bunny.ax != tx || bunny.ay != ty)) {
            doorArr = _doors[bunny.atile.assoc] as Array;
            var gstate :int = Tile.STATE_CLOSED;
            var bunnies :Array = _bn.getBunnies();
            for (var jj :int = 0; jj < bunnies.length; jj++) {
                var bun :Bunny = Bunny(bunnies[jj]);
                if (bun == bunny || bun.atile != bunny.atile) {
                    continue;
                }
                gstate = Tile.STATE_OPENED;
            }
            bunny.atile.setState(gstate);
            if (gstate != Tile.STATE_OPENED) {
                var door :Tile = Tile(doorArr[0])
                _bn.tswitch(false, bunny.ax, bunny.ay, bunny.atile.assoc);
                for (var ii :int = 1; ii < doorArr.length; ii++) {
                    var stile :Tile = Tile(doorArr[ii]);
                    if (stile.state == Tile.STATE_OPENED) {
                        gstate = Tile.STATE_OPENED;
                        break;
                    }
                }
                Tile(doorArr[0]).setState(gstate);
                shadowTile(door);
            }
            bunny.ax = -1;
            bunny.ay = -1;
            bunny.atile = null;
        }
        if (bunny.atile == null && 
                int(_shadow[tx + ty * bwidth]) == Tile.EFFECT_SWITCH) {
            for (ii = 0; ii < _doors.length; ii++) {
                doorArr = _doors[ii] as Array;
                for (jj = 1; jj < doorArr.length; jj++) {
                    stile = Tile(doorArr[jj]);
                    if (stile.x == tx && stile.y == ty) {
                        bunny.ax = tx;
                        bunny.ay = ty;
                        bunny.atile = stile;
                        _bn.tswitch(true, tx, ty, ii);
                        break;
                    }
                }
                if (bunny.atile != null) {
                    break;
                }
            }
        }
        by = (ty - 1) * Tile.TILE_SIZE;
        if (bunny.atile != null && bunny.atile.state == Tile.STATE_OPENED) {
            by += 3;
        }
        bunny.setCoords(bx, by);
    }

    public function climb (bunny :Bunny, delta :int, width :int) :Boolean
    {
        delta = Math.max(Math.min(delta, Tile.TILE_SIZE-1), -Tile.TILE_SIZE+1);
        var bx :int = bunny.getBX();
        var by :int = bunny.getBY();
        var ladderWidth :int = Tile.TILE_SIZE * Tile.LADDER_SIZE;
        var tx :int = int(bx + Tile.TILE_SIZE/2) / Tile.TILE_SIZE;
        var tx2 :int = tx + 1;
        var ty :int = int((by + Tile.TILE_SIZE - 1)/ Tile.TILE_SIZE);
        var tyd :int = int((by + Tile.TILE_SIZE - 1 + delta) / Tile.TILE_SIZE);
        var shadow :Boolean = 
                int(_shadow[tx + ty * bwidth]) == Tile.EFFECT_LADDER &&
                int(_shadow[tx2 + ty * bwidth]) == Tile.EFFECT_LADDER;
        var shadowd :Boolean = 
                int(_shadow[tx + tyd * bwidth]) == Tile.EFFECT_LADDER &&
                int(_shadow[tx2 + tyd * bwidth]) == Tile.EFFECT_LADDER;
        var isClimbing :Boolean = true;
        bx = tx * Tile.TILE_SIZE + (ladderWidth - width)/2;
        if (ty == tyd && shadow) {
            by += delta;
            isClimbing = true;
        } else if (delta < 0) {
            if (!shadow) {
                return false;
            } else if (!shadowd) {
                by = tyd * Tile.TILE_SIZE;
                isClimbing = false;
            } else {
                by += delta;
            }
        } else {
            if (!shadow && !shadowd) {
                return false;
            } else if (!shadowd) {
                by = ty * Tile.TILE_SIZE;
                isClimbing = !canWalk(tx, ty);
            } else {
                by += delta;
            }
        }
        bunny.setCoords(bx, by);
        return isClimbing;
    }

    protected function shadowTile (tile :Tile) :void
    {
        for (var y :int = 0; y < tile.height; y++) {
            for (var x :int = 0; x < tile.width; x++) {
                var idx :int = tile.x + x + (tile.y + y) * bwidth;
                _shadow[idx] = tile.getShadow(int(_shadow[idx]), x, y);
            }
        }
    }

    protected function canPass (x :int, y :int) :Boolean
    {
        var shadow :int = int(_shadow[x + y * bwidth]);
        return shadow == Tile.EFFECT_NONE || shadow == Tile.EFFECT_LADDER ||
            shadow == Tile.EFFECT_DOOR_OPENED;
    }

    protected function canWalk (x :int, y :int) :Boolean
    {
        var shadow :int = int(_shadow[x + y * bwidth]);
        return shadow != Tile.EFFECT_NONE;
    }

    protected var _bn :BunnyKnights;
    protected var _shadow :Array;
    protected var _tiles :Array;
    protected var _doors :Array;
}
}
