package {

import flash.display.Graphics;
import flash.display.Sprite;
import mx.core.BitmapAsset;


public class Board extends Sprite
{
    // PROPERTIES

    /** Number of letters per side of the square board */
    public static const LETTER_COUNT_PER_SIDE : int = 5;

    /** Width and height of each letter cell, in pixels */
    public static const LETTER_SIZE : int = 50;

    /** Width and height of the square playing board, in pixels.
        Letter cells will be centered inside this board. */
    public static const BOARD_SIZE : int = 300;


    // PUBLIC FUNCTIONS

    /**
       Creates a new square board, based on the built-in constants.
    */
    public function Board (game : TangleWord, count : int)
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
        initializeCells (LETTER_COUNT_PER_SIDE);
        
    }



    // PRIVATE HELPERS

    /** Creates a bunch of cells */
    private function initializeCells (count : int) : void
    {
        // Precompute some layout parameters
        var borderSize : int = (BOARD_SIZE - (LETTER_SIZE * LETTER_COUNT_PER_SIDE)) / 2;

        // Create cells
        _cells = new Array (count);
        for (var col : int = 0; col < count; col++)
        {
            _cells[col] = new Array (count);
            for (var row : int = 0; row < count; row++)
            {
                // Figure out cell position
                var x : int = borderSize + col * LETTER_SIZE;
                var y : int = borderSize + row * LETTER_SIZE; 
                _cells[col][row] = new Letter (_game, "X", x, y, LETTER_SIZE, LETTER_SIZE);
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
