package {

import flash.display.Sprite;

public class SeaDisplay extends Sprite
{
    /** The size of a tile. */
    public static const TILE_SIZE :int = 24;

    public function SeaDisplay ()
    {
        graphics.beginFill(0x009999);
        graphics.drawRect(0, 0, TILE_SIZE * Board.SIZE, TILE_SIZE * Board.SIZE);
    }
}
}
