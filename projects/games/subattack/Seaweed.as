package {

import flash.display.Sprite;

public class Seaweed extends Sprite
{
    public function Seaweed ()
    {
        graphics.beginFill(0x1199FF, .5);
        graphics.drawRect(0, 0, SubAttack.TILE_SIZE, SubAttack.TILE_SIZE);
    }
}
}
