package
{


import flash.geom.Point;


/** The Controller class holds game logic, and updates game state in the model. */
    
public class Controller 
{
    // PUBLIC METHODS
    
    public function Controller (model : Model) : void
    {
        _model = model;

    }

    public function setModel (model : Model) : void
    {
        _model = model;
    }



    /** Takes a new letter from the UI, and checks it against game logic. */
    public function tryAddLetter (position : Point) : void
    {
        // Position of the letter on top of the stack 
        var lastLetterPosition : Point = _model.getLastLetterPosition ();

        // Did the player click on the first letter? If so, just add it.
        var noPreviousLetterFound : Boolean = (lastLetterPosition == null);
        if (noPreviousLetterFound)
        {
            _model.selectLetterAtPosition (position);
            return;
        }

        // Did the player click on the last letter they added? If so, remove it.
        if (position.equals (lastLetterPosition))
        {
            _model.removeLastSelectedLetter ();
            return;
        }

        // Did the player click on an empty letter next to the last selected one?
        var isValidNeighbor : Boolean = (areNeighbors (position, lastLetterPosition) &&
                                   ! _model.isLetterSelectedAtPosition (position));
        if (isValidNeighbor)
        {
            _model.selectLetterAtPosition (position);
            return;
        }

        // Don't do anything
    }

    /** Randomizes the game board, based on a flat array of letters. */
    public function setGameBoard (s : Array) : void
    {
        // Copy them over to the data set
        for (var x : int = 0; x < Properties.LETTERS; x++)
        {
            for (var y : int = 0; y < Properties.LETTERS; y++)
            {
                _model.updateBoardLetter (new Point (x, y), s [x * Properties.LETTERS + y]);
            }
        }
    }


    // PRIVATE METHODS

    /** Determines whether the given /position/ is a neighbor of specified /original/
        position (defined as being one square away from each other). */
    private function areNeighbors (position : Point, origin : Point) : Boolean
    {
        return (! position.equals (origin) &&
                Math.abs (position.x - origin.x) <= 1 &&
                Math.abs (position.y - origin.y) <= 1);
    }
    
    
    // PRIVATE VARIABLES

    /** Game data interface */
    private var _model : Model;

}

}

