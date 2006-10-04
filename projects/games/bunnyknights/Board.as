package {

import flash.display.Sprite;

public class Board
{
    public static const BOARD_WIDTH :int = 20;
    public static const BOARD_HEIGHT :int = 15;

    public function Board (bn :BunnyKnights)
    {
        _bn = bn;
        _shadow = new Array(BOARD_WIDTH * BOARD_HEIGHT);
        _tiles = new Array();
    }

    public function addTile (tile :Tile) :void
    {
        _tiles.push(tile);
        var tileSprite :Sprite = tile.getSprite();
        tileSprite.x = tileSprite.width * tile.x;
        tileSprite.y = tileSprite.height * tile.y;
        _bn.addChildToLayer(tileSprite, tile.layer);
        _shadow[tile.x + tile.y * BOARD_HEIGHT] = tile.effect;
    }

    protected var _bn :BunnyKnights;
    protected var _shadow :Array;
    protected var _tiles :Array;
}
}
