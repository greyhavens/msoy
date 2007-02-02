package {

import flash.display.Graphics;
import flash.display.Sprite;
import flash.geom.Point;
import mx.core.BitmapAsset;


public class Board extends Sprite
{

    // PUBLIC FUNCTIONS

    /**
       Creates a new square board, based on the built-in constants.
    */
    public function Board (game : TangleWord)
    {
        selection = new Selection (this);
        _game = game;
        _background = Resources.makeDefaultBoardImage ();
        if (_background == null)
        {
            throw new Error ("Failed to load embedded board background!");
        }

        // Let's initialize what needs to be initialized
        addChild (_background);
        initializeCells (Properties.LETTER_COUNT_PER_SIDE);
        
    }

    /**
       Used by the game to populate the board with a new set of letters.
       /letters/ is an array of strings, of the same length as the total
       number of cells is the collection.
    */
    public function updateLetters (letters : Array) : void
    {
        Assert.True (function () : Boolean {
                        return letters.length == Properties.LETTER_COUNT_TOTAL; },
                     "Something happened to the letters array! The size is all wrong.");

        // Go through each letter and update its string.
        for (var col : int = 0; col < Properties.LETTER_COUNT_PER_SIDE; col++)
        {
            for (var row : int = 0; row < Properties.LETTER_COUNT_PER_SIDE; row++)
            {
                var currentIndex : int = col * Properties.LETTER_COUNT_PER_SIDE + row;
                var letter : String = letters[currentIndex];
                _cells[col][row].setText (letter);
            }
        }
    }

    /**
       Used to query the board, and see if the particular sequence of strings
       occurs in the cell matrix. /letters/ should be an array of strings
       corresponding to the individual letters (or digraphs, if applicable).
    */
    public function checkBoard (letters : Array) : Boolean
    {
        var word : String = selectionAsString ();
        var isValid : Boolean = DictionaryService.checkWord (0, word);
        trace ((isValid ? "MATCH " : "FAIL ") + word);

        // Depending on the result, we may want to update all of the selected letters
        var getLetter : Function = function (p : Point, i : int, a : Array) : Letter
        {
            return _cells[p.x][p.y];
        }
        var letterObjectArray : Array = selection.elements.map (getLetter);
        var resultStatus : uint = isValid ? Status.MATCHING : Status.NORMAL;
        for each (var letter : Letter in letterObjectArray)
        {
            letter.setStatus (resultStatus);
        }

        // Now clean out the selection array
        selection.elements = new Array ();  // why isn't there an Array.clear()?
        updateDisplay();
        
        return isValid;
    }

    /** Updates a text box with the current word */
    public function updateDisplay () : void
    {
        var word : String = selectionAsString ();
        _game.setText (word);
    }

    
    // PRIVATE HELPERS

    /** Creates a bunch of cells */
    private function initializeCells (count : int) : void
    {
        // Precompute some layout parameters
        var letterBoxSize : int = Properties.LETTER_SIZE * Properties.LETTER_COUNT_PER_SIDE;
        var xOffset : int = Properties.BOARD.x + (Properties.BOARD.width - letterBoxSize) / 2;
        var yOffset : int = Properties.BOARD.y + (Properties.BOARD.height - letterBoxSize) / 2;
    
        // Create cells
        _cells = new Array (count);
        for (var col : int = 0; col < count; col++)
        {
            _cells[col] = new Array (count);
            for (var row : int = 0; row < count; row++)
            {
                // Figure out cell position
                var x : int = xOffset + col * Properties.LETTER_SIZE;
                var y : int = yOffset + row * Properties.LETTER_SIZE; 
                _cells[col][row] = new Letter (this, _game, "?", x, y, col, row);
                addChild (_cells[col][row]);
            }
        }
    }

    /** Retrieves a given cell's contents */
    private function getText (x : int, y : int) : String
    {
        return _cells[x][y].text;
    }

    /** Converts the selection point stack to a string */
    private function selectionAsString () : String
    {
        var getText : Function = function (p : Point, i : int, a : Array) : String
        {
            return _cells[p.x][p.y].getText();
        }
        var word : String = selection.elements.map(getText).join("");
        return word;
    }
    
    

    // PUBLIC MEMBER FIELDS

    /** Current selection */
    public var selection : Selection;
    
    

    // PRIVATE MEMBER FIELDS

    /** Pointer back to the game object */
    private var _game : TangleWord;

    /** Number of letters per side of the square. Total count is _sidecount^2 */
    private var _sidecount : int;

    /** Background of the board. */
    private var _background : BitmapAsset;

    /** This 2D array contains letters on the board.
        The first index is the column, e.g. _cells[x][y] */
    private var _cells : Array;


}



} // package
