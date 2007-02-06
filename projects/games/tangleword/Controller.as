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
    public function selectLetter (position : Point, name : String) : void
    {
        
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

    
    // PRIVATE VARIABLES

    /** Game data interface */
    private var _model : Model;

}

}

