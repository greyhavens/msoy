package
{

import flash.display.Sprite;    
import flash.display.Graphics;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;
import mx.core.BitmapAsset;


/** The Display class represents the game visualization, including UI
    and game state display. */
public class Display extends Sprite
{


    // PUBLIC FUNCTIONS

    /** Initializes the board and everything on it */
    public function Display (controller : Controller) : void
    {
        // Copy parameters
        _controller = controller;

        // Initialize the background bitmap
        _background = Resources.makeGameBackground ();
        Assert.NotNull (_background, "Background bitmap failed to initialize!"); 
        addChild (_background);
        
        // Initialize empty letters
        initializeLetters ();

        // Register for events
        addEventListener (MouseEvent.CLICK, clickHandler);
        addEventListener (MouseEvent.MOUSE_MOVE, mouseHandler);
        // addEventListener (Event.ENTER_FRAME, frameHandler);
        
    }

    /** Called from the model, this accessor modifies the display /text/
        for one letter at specified board /position/. */
    public function setText (position : Point, text : String) : void
    {
        Assert.True (isValidBoardPosition (position),
                     "Bad position received in Display:setText");
        _letters[position.x][position.y].setText (text);
    }

    


    // PRIVATE EVENT HANDLERS

    private function clickHandler (event : MouseEvent) : void
    {
        var p : Point = new Point (event.stageX, event.stageY);
        var i : Point = screenToBoard (p);
        if (i != null)
        {
            
        }
    }
        
    private function mouseHandler (event : MouseEvent) : void
    {
        var p : Point = new Point (event.stageX, event.stageY);
        var i : Point = screenToBoard (p);
        highlightLetter (i);
    }

    private function tickHandler (event : Event) : void
    {
    }



    // PRIVATE HELPER FUNCTIONS

    
    /** Initializes storage, and creates letters at specified positions on the board */
    private function initializeLetters () : void
    {
        // Create the 2D array
        var count : int = Properties.LETTERS;
        _letters = new Array (count);
        for (var x : int = 0; x < count; x++)
        {
            _letters[x] = new Array (count);
            for (var y : int = 0; y < count; y++)
            {
                var l : Letter = new Letter (this);   // make a new instance
                var p : Point = boardToScreen (new Point (x, y));
                l.x = p.x;
                l.y = p.y;
                addChild (l);          // add to display
                _letters[x][y] = l;    // add to list
            }
        }
    }

    /**
       Highlights the letter at specified board /location/, and removes the highlight
       from the previous letter. If the location point is null, it just removes
       the highlight from the previous letter.
    */
    private function highlightLetter (location : Point) : void
    {
        if (location != null)
        {
            Assert.Fail ("Highlighting letter: " + location.toString());
        }
        else
        {
            Assert.Fail ("Highlight RESET!");
        }
        
        var l : Letter = null;
        
        if (location != null &&
            _lastHighlight != null &&
            location.equals (_lastHighlight))
        {
            // Highlight hasn't changed; ignore.
            return;
        }

        // Remove old highlight, if any
        if (_lastHighlight != null)
        {
            l = _letters[_lastHighlight.x][_lastHighlight.y];
            l.isCursorEnabled = false;
            _lastHighlight = null;
        }

        // Set the new highlight
        if (location != null)
        {
            l = _letters[location.x][location.y];
            l.isCursorEnabled = true;
            _lastHighlight = location;
        }
    }        


    /** Helper function: converts screen coordinate to a board square position.
        If the screen coordinate falls outside the board, returns /null/. */
    private function screenToBoard (p : Point) : Point
    {
        // remove offset
        var newp : Point = new Point (p.x - Properties.BOARD.x, p.y - Properties.BOARD.y);

        // convert to board coordinates
        newp.x = Math.floor (newp.x / Properties.LETTER_SIZE);
        newp.y = Math.floor (newp.y / Properties.LETTER_SIZE);

        // check bounds and return
        if (! isValidBoardPosition (newp))
        {
            return null;
        }

        return newp;        
    }

    /** Helper function: converts board square coordinate into the screen coordinates
        of the upper left corner of that square. If the board position falls outside
        the board, returns /null/. */
    private function boardToScreen (p : Point) : Point
    {
        if (! isValidBoardPosition (p))
        {
            return null;
        }

        var p : Point = new Point (p.x * Properties.LETTER_SIZE + Properties.BOARD.x,
                                   p.y * Properties.LETTER_SIZE + Properties.BOARD.y);

        return p;
    }

    /** Checks if a given point is inside board dimension bounds */
    private function isValidBoardPosition (p : Point) : Boolean
    {
        return (p.x >= 0 && p.x < Properties.LETTERS &&
                p.y >= 0 && p.y < Properties.LETTERS);
    }
        
    

    // PRIVATE VARIABLES

    /** Game logic */
    private var _controller : Controller;

    /** Overall game background */
    private var _background : BitmapAsset;

    /** Storage for each letter object */
    private var _letters : Array;

    /** Board position of the currently highlighted letter */
    private var _lastHighlight : Point;
    
}

}

