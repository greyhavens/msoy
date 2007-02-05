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

        _input = new InputField (this);
        addChild (_input);

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

    // Happens when the player makes a guess - this checks the board,
    // and will eventually do something
    public function checkBoard (letters : Array) : void
    {
        var result : Boolean = _board.checkBoard (letters);
    }

    // Happens when the player updates the letter selection
    public function setText (text : String) : void
    {
        _input.setText (text);
    }

    // PRIVATE HELPERS
    private function populateBoard () : void
    {
        var letters : Array = DictionaryService.getLetterSet (0, Properties.LETTER_COUNT_TOTAL);
        _board.updateLetters (letters);
    }

    private var _board : Board;
    private var _input : InputField;

}

}
