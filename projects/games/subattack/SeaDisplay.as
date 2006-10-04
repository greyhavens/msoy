package {

import flash.display.Sprite;

import flash.events.Event;

public class SeaDisplay extends Sprite
{
    /** The size of a tile. */
    public static const TILE_SIZE :int = 32;

    public function SeaDisplay ()
    {
        graphics.beginFill(0x00CC66);
        graphics.drawRect(0, 0,
            TILE_SIZE * Board.WIDTH, TILE_SIZE * Board.HEIGHT);
        graphics.endFill();

        graphics.lineStyle(2, 0x000000);
        graphics.drawRect(0, 0,
            TILE_SIZE * Board.WIDTH, TILE_SIZE * Board.HEIGHT);
        graphics.lineStyle(0, 0, 0);

//        addEventListener(Event.ENTER_FRAME, enterFrame);
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
        graphics.beginFill(0x009999);
        graphics.drawRect(xx * TILE_SIZE, yy * TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    protected function enterFrame (event :Event) :void
    {
        // find out where our sub is and visualize that
        var vx :int = _sub.getX() - SubAttack.VISION_TILES;
        var vy :int = _sub.getY() - SubAttack.VISION_TILES;
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

    /** The submarine that we're following. */
    protected var _sub :Submarine;
}
}
