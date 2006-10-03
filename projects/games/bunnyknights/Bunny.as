package {

import flash.display.Sprite;
import mx.core.BitmapAsset;

public class Bunny extends Sprite
{
    public function Bunny ()
    {
        var idleBunny :BitmapAsset = BitmapAsset(new bunnyIdle());
        addChild(idleBunny);
    }

    [Embed(source="rsrc/bunny/bunny_knight_blue_idle.gif")]
    protected var bunnyIdle :Class;
}
}
