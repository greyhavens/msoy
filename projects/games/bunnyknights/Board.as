package {

import flash.display.Graphics;
import flash.display.Sprite;

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
    }

    public function addTile (tile :Tile) :void
    {
        _tiles.push(tile);
        if (tile.effect <= Tile.EFFECT_SOLID) {
            var layerSprite :Sprite = _bn.getLayer(tile.layer);
            layerSprite.graphics.beginBitmapFill(tile.getBitmapData());
            layerSprite.graphics.drawRect(tile.x * Tile.TILE_SIZE,
                tile.y * Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE);
            layerSprite.graphics.endFill();
        } else {
            var tileSprite :Sprite = tile.getSprite();
            tileSprite.x = tileSprite.width * tile.x;
            tileSprite.y = tileSprite.height * tile.y;
            _bn.addChildToLayer(tileSprite, tile.layer);
        }
        _shadow[tile.x + tile.y * bwidth] = tile.effect;
    }

    public function addBunny (bunny :Bunny, x :int, y :int) :void
    {
        bunny.setCoords(x * Tile.TILE_SIZE, y * Tile.TILE_SIZE);
        _bn.addChildToLayer(bunny, Tile.LAYER_ACTION_FRONT);
    }

    public function move (bunny :Bunny, delta :int, width :int) :void
    {
        // don't allow a movement longer than 1 tile until I implment
        // proper collision detection
        delta = Math.max(Math.min(delta, Tile.TILE_SIZE), -Tile.TILE_SIZE);
        var bx :int = bunny.getBX();
        var by :int = bunny.getBY();
        var tx1 :int = int((bx + delta) / Tile.TILE_SIZE);
        var tx2 :int = int((bx + delta + width) / Tile.TILE_SIZE);
        var ty :int = int(by / Tile.TILE_SIZE);
        if (int(_shadow[tx1 + ty * bwidth]) == Tile.EFFECT_SOLID) {
            if (int(bx / Tile.TILE_SIZE) < tx1) {
                bx = tx1 * Tile.TILE_SIZE - width;
            } else {
                bx = (tx1 + 1) * Tile.TILE_SIZE;
            }
        } else if (int(_shadow[tx2 + ty * bwidth]) == Tile.EFFECT_SOLID) {
            if (int((bx + width) / Tile.TILE_SIZE) < tx2) {
                bx = tx2 * Tile.TILE_SIZE - width - 1;
            } else {
                bx = tx2 * Tile.TILE_SIZE;
            }
        } else {
            bx += delta;
        }
        bunny.setCoords(bx, by);
    }

    protected var _bn :BunnyKnights;
    protected var _shadow :Array;
    protected var _tiles :Array;
}
}
