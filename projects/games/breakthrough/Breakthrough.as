package {

import flash.display.Sprite;

import com.threerings.ezgame.EZGameControl;

[SWF(width="300", height="400")]
public class Breakthrough extends Sprite
{
    public function Breakthrough ()
    {
        // create the game board
        addChild(new Board(new EZGameControl(this)));
    }
}
}
