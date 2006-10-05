package {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Sprite;

import flash.events.Event;

import flash.text.TextField;

public class SeaDisplay extends Sprite
{
    /** The size of a tile. */
    public static const TILE_SIZE :int = 32;

    public function SeaDisplay ()
    {
        /**
        graphics.beginFill(0x00CC66);
        graphics.drawRect(0, 0,
            TILE_SIZE * Board.WIDTH, TILE_SIZE * Board.HEIGHT);
        graphics.endFill();
        */

        var ups :Array = [];
        ups[0] = Bitmap(new _up1()).bitmapData;
        ups[1] = Bitmap(new _up2()).bitmapData;
        ups[2] = Bitmap(new _up3()).bitmapData;
        ups[3] = Bitmap(new _up4()).bitmapData;

        _downs[0] = Bitmap(new _down1()).bitmapData;
        _downs[1] = Bitmap(new _down2()).bitmapData;
        _downs[2] = Bitmap(new _down3()).bitmapData;
        _downs[3] = Bitmap(new _down4()).bitmapData;

        for (var yy :int = 0; yy < Board.HEIGHT; yy++) {
            for (var xx :int = 0; xx < Board.WIDTH; xx++) {
                pickBitmap(ups);
                graphics.drawRect(xx * TILE_SIZE, yy * TILE_SIZE, TILE_SIZE,
                    TILE_SIZE);
            }
        }

//        graphics.lineStyle(2, 0x000000);
//        graphics.drawRect(0, 0,
//            TILE_SIZE * Board.WIDTH, TILE_SIZE * Board.HEIGHT);
//        graphics.lineStyle(0, 0, 0);

        _respawnMsg = new TextField();
        _respawnMsg.text = "Press ENTER to spawn.";
        _respawnMsg.background = true;
    }

    public function setFollowSub (sub :Submarine) :void
    {
        _sub = sub;
        subUpdated(sub, sub.getX(), sub.getY());
    }

    /**
     * Display the specified tile as now being traversable.
     */
    public function markTraversable (xx :int, yy :int) :void
    {
        //graphics.beginFill(0x009999);
        //graphics.beginBitmapFill(_downs);
        pickBitmap(_downs);
        graphics.drawRect(xx * TILE_SIZE, yy * TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    public function subUpdated (sub :Submarine, xx :int, yy :int) :void
    {
        if (_sub != sub) {
            return;
        }

        var vx :int = xx - SubAttack.VISION_TILES;
        var vy :int = yy - SubAttack.VISION_TILES;
        if (vx < 0) {
            vx = 0;
        } else if (vx > Board.WIDTH - SubAttack.VIEW_TILES) {
            vx = Board.WIDTH - SubAttack.VIEW_TILES;
        }
        if (vy < 0) {
            vy = 0;
        } else if (vy > Board.HEIGHT - SubAttack.VIEW_TILES) {
            vy = Board.HEIGHT - SubAttack.VIEW_TILES;
        }

        // update OUR location..
        x = vx * -1 * TILE_SIZE;
        y = vy * -1 * TILE_SIZE;
    }

    public function checkSubDeath (sub :Submarine) :void
    {
        if (sub != _sub) {
            return;
        }
        var isDead :Boolean = sub.isDead();
        if (isDead == (_respawnMsg.parent == null)) {
            if (isDead) {
                _respawnMsg.x = sub.x;
                _respawnMsg.y = sub.y;
                addChild(_respawnMsg);
            } else {
                removeChild(_respawnMsg);
            }
        }
    }

    protected function pickBitmap (choices :Array) :void
    {
        var n :Number = Math.random();
        var ii :int;
        for (ii = 0; ii < PICKS.length - 1; ii++) {
            if (n <= PICKS[ii]) {
                break;
            }
        }
        graphics.beginBitmapFill(BitmapData(choices[ii]));
    }

    /** The submarine that we're following. */
    protected var _sub :Submarine;

    protected var _downs :Array = [];

    protected var _respawnMsg :TextField;

    /** The frequency with which to pick each bitmap. Must add to 1.0 */
    protected static const PICKS :Array = [ 0.05, 0.15, 0.30, 0.50 ];

    [Embed(source="up_01.png")]
    protected static const _up1 :Class;

    [Embed(source="up_02.png")]
    protected static const _up2 :Class;

    [Embed(source="up_03.png")]
    protected static const _up3 :Class;

    [Embed(source="up_04.png")]
    protected static const _up4 :Class;

    [Embed(source="down_01.png")]
    protected static const _down1 :Class;

    [Embed(source="down_02.png")]
    protected static const _down2 :Class;

    [Embed(source="down_03.png")]
    protected static const _down3 :Class;

    [Embed(source="down_04.png")]
    protected static const _down4 :Class;
}
}
