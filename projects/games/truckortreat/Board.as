package {

import flash.display.Sprite;

import com.threerings.ezgame.EZGame;

public class Board extends Sprite
{
    /** Number of pixels wide and high each cell is. */
    public static const CELL_SIZE :int = 32;
    
    /** Cell dimensions of board. */
    public static const WIDTH :int = 16;
    public static const HEIGHT :int = 16;
    
    public function Board (gameObj :EZGame)
    {
        graphics.clear();
        // Draw an exciting background.
        graphics.beginFill(0xC9C9C9);
        graphics.drawRect(0, 0, CELL_SIZE * WIDTH, CELL_SIZE * HEIGHT);
    }
}
}
