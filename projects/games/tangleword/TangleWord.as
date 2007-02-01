package {

import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;
import flash.geom.Point;

import com.threerings.ezgame.EZGameControl;


[SWF(width="500", height="500")]
public class TangleWord extends Sprite 
{
    // PUBLIC METHODS
    public function TangleWord () : void
    {

        // Initialize board visuals
        _board = new Board (this);
        addChild (_board);

        // Initialize mask based on the board
        var masker :Shape = new Shape();
        masker.graphics.beginFill (0xFFFFFF);
        masker.graphics.drawRect (
            Properties.DISPLAY.x, Properties.DISPLAY.y,
            Properties.DISPLAY.width, Properties.DISPLAY.height);
        masker.graphics.endFill ();
        this.mask = masker;
        addChild(masker); // the mask must be added to the display

        // Populate the board
        populateBoard ();
        
    }

    // PRIVATE HELPERS
    private function populateBoard () : void
    {
        var letters : Array = DictionaryService.getLetterSet (0, Properties.LETTER_COUNT_TOTAL);
        _board.updateLetters (letters);
    }

    private var _board : Board;

}

}
