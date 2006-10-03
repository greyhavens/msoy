package {

import flash.display.Sprite;

public class Submarine extends Sprite
{
    public function Submarine (playerIdx :int)
    {
        graphics.beginFill((playerIdx == 0) ? 0xFFFF00 : 0x00FFFF);
        graphics.drawCircle(SubAttack.TILE_SIZE / 2, SubAttack.TILE_SIZE / 2,
            SubAttack.TILE_SIZE / 2);
    }
}
}
