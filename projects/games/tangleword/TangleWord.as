package {

import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import com.threerings.ezgame.EZGameControl;


[SWF(width="500", height="500")]
public class TangleWord extends Sprite 
{
    public function TangleWord () : void
    {

        // Initialize the board
        _board = new Board (this, 200);
        addChild (_board);

        // Initialize mask based on the board
        var masker :Shape = new Shape();
        masker.graphics.beginFill (0xFFFFFF);
        masker.graphics.drawRect (0, 0, Board.BOARD_SIZE, Board.BOARD_SIZE);
        masker.graphics.endFill ();
        this.mask = masker;
        addChild(masker); // the mask must be added to the display

        
    }

    private var _board : Board;

}

}
