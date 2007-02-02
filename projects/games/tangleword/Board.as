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
        // Initialize a stack of Points, which will hold the previously traversed path.
        var alreadyVisited : Array = new Array ();
        for (var x : int = 0; x < Properties.LETTER_COUNT_PER_SIDE; x++)
        {
            for (var y : int = 0; y < Properties.LETTER_COUNT_PER_SIDE; y++)
            {
                var p : Point = new Point (x, y);
                if (matchWord (p, letters, 0, new Array ()))
                {
                    // abort and report success!
                    trace ("MATCH: " + letters.toString());
                    return true;
                }
            }
        }

        return false;
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
                _cells[col][row] = new Letter (this, _game, "?", x, y);
                addChild (_cells[col][row]);
            }
        }
    }

    /** Retrieves a given cell's contents */
    private function getText (x : int, y : int) : String
    {
        return _cells[x][y].text;
    }

    /**
       Private helper that actually does the recursive search through the board.
       At each step, consider the next remaining letter to be matched,
       and try it against every neighbor that's not on the already visited list.
       Whenever successful, keep recursing until we run out of letters.

       /p/ is the location of the current cell.
       /letters/ is an array of strings.
       /current/ is the position of the letter being examined right now.
       /alreadyVisited/ is an array of Points.
    */
    private function matchWord (p : Point, letters : Array,
                                current: int, alreadyVisited : Array) : Boolean
    {
        // Are we done? Hooray!
        if (current == letters.length)
        {
            return true;
        }
        
        var thisLetter : String = _cells[p.x][p.y].getText ();
        var candidateLetter : String = letters[current];

        // trace (current + ": checking " + thisLetter + " at " + p.x + ", " + p.y);
        
        // If the letter doesn't match this cell, fail.
        if (thisLetter != candidateLetter)
        {
            return false;
        }

        // If this position was visited before, fail.
        var pointMatches : Function =
            function (oldpoint : Point, index : int, array : Array) : Boolean
            {
                var samePoint : Boolean = (p.x == oldpoint.x && p.y == oldpoint.y);
                return (samePoint);
            }
        if (alreadyVisited.some (pointMatches))
        {
            return false;
        }

        // So far, so good. Now let's remember where we are,
        // and do the same check for each of the neighbors.
        alreadyVisited.push (p);
        var indices : Array = new Array (-1, 0, 1);
        for each (var dx : int in indices) {
            for each (var dy : int in indices) {
                var newx : int = p.x + dx;
                var newy : int = p.y + dy;
                if (! (newx == p.x && newy == p.y) &&    // it's not this cell
                    newx >= 0 && newx < Properties.LETTER_COUNT_PER_SIDE &&  // not out of bounds
                    newy >= 0 && newy < Properties.LETTER_COUNT_PER_SIDE)
                {
                  var newp : Point = new Point (newx, newy);
                    
                    // Check if one of our neighbors leads to a match - if so, we're done!
                    if (matchWord (newp, letters, current + 1, alreadyVisited))
                    {
                        return true;
                    }
                }
                }
            }

        // Nothing matched. Clean up and quit.
        var oldp : Point = alreadyVisited.pop ();
        Assert.True (function () : Boolean { return oldp == p; },
                     "Error in matchWord recursion!");

        return false;

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
