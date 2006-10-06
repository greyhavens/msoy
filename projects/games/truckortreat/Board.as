package {

import flash.display.Sprite;

import com.threerings.ezgame.EZGame;

public class Board extends Sprite
{
    /** Pixel dimensions of board. */
    public static const WIDTH :int = 512;
    public static const HEIGHT :int = 512;
    
    /** Lists of sidewalk coordinates. These are safe spots to start on. */
    public static const SIDEWALK_X :Array = [];
    public static const SIDEWALK_Y :Array = [];
    
    public function Board (gameObj :EZGame)
    {
        graphics.clear();
        // Draw an exciting background.
        graphics.beginFill(0xC9C9C9);
        graphics.drawRect(0, 0, WIDTH, HEIGHT);
    }
}
}
