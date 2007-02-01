package {

import flash.display.Graphics;
import flash.display.Sprite;
import mx.core.BitmapAsset;


public class Board extends Sprite
{

    // PUBLIC FUNCTIONS

    /**
       Creates a new square board, based on the built-in constants.
    */
    public function Board (game : TangleWord)
    {
        _game = game;
        _background = Resources.makeDefaultBoardImage ();

        // Is everything ready?
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
       Expects an array of the same size as the total letter count. */
    public function updateLetters (letters : Array) : void
    {
        // TODO: Assert here letters.length == Properties.

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
        


    // PRIVATE HELPERS

    /** Creates a bunch of cells */
    private function initializeCells (count : int) : void
    {
        // Precompute some layout parameters
        var letterBoxSize : int = Properties.LETTER_SIZE * Properties.LETTER_COUNT_PER_SIDE;
        var xBorderSize : int = (Properties.BOARD.width - letterBoxSize) / 2;
        var yBorderSize : int = (Properties.BOARD.height - letterBoxSize) / 2;
    
        // Create cells
        _cells = new Array (count);
        for (var col : int = 0; col < count; col++)
        {
            _cells[col] = new Array (count);
            for (var row : int = 0; row < count; row++)
            {
                // Figure out cell position
                var x : int = xBorderSize + col * Properties.LETTER_SIZE;
                var y : int = yBorderSize + row * Properties.LETTER_SIZE; 
                _cells[col][row] = new Letter (_game, "?", x, y);
                addChild (_cells[col][row]);
            }
        }
    }

    /** Retrieves a given cell's contents */
    private function getText (x : int, y : int) : String
    {
        return _cells[x][y].text;
    }

    
    
    
    

    // PRIVATE MEMBER FIELDS

    /** Pointer back to the game object */
    private var _game : TangleWord;

    /** Number of letters per side of the square. Total count is _sidecount^2 */
    private var _sidecount : int;

    /** Background of the board. */
    private var _background : BitmapAsset = null;

    /** This 2D array contains letters on the board.
        The first index is the column, e.g. _cells[x][y] */
    private var _cells : Array = null;


}



} // package
