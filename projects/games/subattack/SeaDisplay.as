package {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Sprite;

import flash.events.Event;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

public class SeaDisplay extends Sprite
{
    /** The size of a tile. */
    public static const TILE_SIZE :int = 32;

    public function SeaDisplay ()
    {
        // set up a status text area, to be centered in the main view
        _status = new TextField();
        _status.multiline = true;
        _status.background = true;
        _status.autoSize = TextFieldAutoSize.CENTER;
        _status.selectable = false;
    }

    /**
     * Configure the initial visualization of the sea.
     */
    public function setupSea (boardWidth :int, boardHeight :int) :void
    {
        var bigups :Array = [
            Bitmap(new UP1()).bitmapData,
            Bitmap(new UP3()).bitmapData,
            Bitmap(new UP5()).bitmapData
        ];

        _ups = [
            Bitmap(new UP2()).bitmapData,
            Bitmap(new UP4()).bitmapData,
            Bitmap(new UP6()).bitmapData,
            Bitmap(new UP7()).bitmapData,
            Bitmap(new UP8()).bitmapData,
            Bitmap(new UP9()).bitmapData,
            Bitmap(new UP10()).bitmapData,
            Bitmap(new UP11()).bitmapData
        ];

        _downs = [
            Bitmap(new DOWN1()).bitmapData,
            Bitmap(new DOWN2()).bitmapData,
            Bitmap(new DOWN3()).bitmapData,
            Bitmap(new DOWN4()).bitmapData
         ];
        _downWall = Bitmap(new DOWN_WALL()).bitmapData;

        for (var yy :int = -SubAttack.VISION_TILES;
                yy < boardHeight + SubAttack.VISION_TILES; yy++) {
            for (var xx :int = -SubAttack.VISION_TILES;
                    xx < boardWidth + SubAttack.VISION_TILES; xx++) {
                pickBitmap(bigups);
                graphics.drawRect(xx * TILE_SIZE, yy * TILE_SIZE, TILE_SIZE,
                    TILE_SIZE);
            }
        }
        graphics.endFill();

        // draw a nice border around Mr. Game Area
        graphics.lineStyle(5, 0xFFFFFF);
        graphics.drawRect(-5, -5, boardWidth * TILE_SIZE + 10,
            boardHeight * TILE_SIZE + 10);
        graphics.lineStyle(0, 0, 0); // turn off lines
    }


    /**
     * Set the status message to be shown over the game board.
     */
    public function setStatus (msg :String) :void
    {
        _status.htmlText = msg;
        _status.x =
            ((SubAttack.VIEW_TILES * TILE_SIZE) - _status.textWidth) / 2;
        _status.y = 
            ((SubAttack.VIEW_TILES * TILE_SIZE) - _status.textHeight) / 2;
        if (_status.parent == null) {
            parent.addChild(_status);
        }
    }

    /**
     * Clear any status message being shown.
     */
    public function clearStatus () :void
    {
        if (_status.parent != null) {
            parent.removeChild(_status);
        }
    }

    /**
     * Set the submarine that we focus on and follow.
     */
    public function setFollowSub (sub :Submarine) :void
    {
        _sub = sub;
        subUpdated(sub, sub.getX(), sub.getY());
    }

    /**
     * Display the specified tile as now being traversable.
     */
    public function markTraversable (
        xx :int, yy :int, level :int,
        aboveIsTrav :Boolean, belowIsTrav :Boolean) :void
    {
        if (level == 1) {
            pickBitmap(_ups);

        } else if (!aboveIsTrav) {
            graphics.beginBitmapFill(_downWall);
        } else {
            pickBitmap(_downs);
        }
        graphics.drawRect(xx * TILE_SIZE, yy * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        if (level == 0 && belowIsTrav) {
            pickBitmap(_downs);
            graphics.drawRect(xx * TILE_SIZE, (yy + 1) * TILE_SIZE,
                TILE_SIZE, TILE_SIZE);
        }
        graphics.endFill();
    }

    /**
     * Called by subs when their location changes.
     */
    public function subUpdated (sub :Submarine, xx :int, yy :int) :void
    {
        if (_sub != sub) {
            return;
        }

        x = (SubAttack.VISION_TILES - xx) * TILE_SIZE;
        y = (SubAttack.VISION_TILES - yy) * TILE_SIZE;
    }

    /**
     * Called by subs when their death state changes.
     */
    public function deathUpdated (sub :Submarine) :void
    {
        // we only care if it's the sub we're watching
        if (sub != _sub) {
            return;
        }

        var isDead :Boolean = sub.isDead();
        if (isDead) {
            setStatus("Press ENTER to respawn.");
        } else {
            clearStatus();
        }
    }

    /**
     * Set the graphics to begin filling a bitmap picked from the
     * specified set.
     */
    protected function pickBitmap (choices :Array) :void
    {
        graphics.beginBitmapFill(
            BitmapData(choices[int(Math.random() * choices.length)]));
//        var n :Number = Math.random();
//        var ii :int;
//        for (ii = 0; ii < PICKS.length - 1; ii++) {
//            n -= PICKS[ii];
//            if (n <= 0) {
//                break;
//            }
//        }
//        graphics.beginBitmapFill(BitmapData(choices[ii]));
    }

    /** The submarine that we're following. */
    protected var _sub :Submarine;

    protected var _downs :Array;

    protected var _ups :Array;

    protected var _downWall :BitmapData;

    /** Our status message. */
    protected var _status :TextField;

    /** The frequency with which to pick each bitmap. Must add to 1.0 */
    protected static const PICKS :Array = [ 0.10, 0.20, 0.30, 0.40 ];

    [Embed(source="up_01.png")]
    protected static const UP1 :Class;

    [Embed(source="up_02.png")]
    protected static const UP2 :Class;

    [Embed(source="up_03.png")]
    protected static const UP3 :Class;

    [Embed(source="up_04.png")]
    protected static const UP4 :Class;

    [Embed(source="up_05.png")]
    protected static const UP5 :Class;

    [Embed(source="up_06.png")]
    protected static const UP6 :Class;

    [Embed(source="up_07.png")]
    protected static const UP7 :Class;

    [Embed(source="up_08.png")]
    protected static const UP8 :Class;

    [Embed(source="up_09.png")]
    protected static const UP9 :Class;

    [Embed(source="up_10.png")]
    protected static const UP10 :Class;

    [Embed(source="up_11.png")]
    protected static const UP11 :Class;

    [Embed(source="down_wall.png")]
    protected static const DOWN_WALL :Class;

    [Embed(source="down_01.png")]
    protected static const DOWN1 :Class;

    [Embed(source="down_02.png")]
    protected static const DOWN2 :Class;

    [Embed(source="down_03.png")]
    protected static const DOWN3 :Class;

    [Embed(source="down_04.png")]
    protected static const DOWN4 :Class;
}
}
