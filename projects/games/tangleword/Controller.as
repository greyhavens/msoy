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


    /** Randomizes the game board */
    public function randomizeGameBoard () : void
    {
        // Retrieve a bag of random letters
        var s : Array = DictionaryService.getLetterSet (LocaleSettings.EN_US,
                                                          Properties.LETTER_COUNT);
        Assert.True (s.length == Properties.LETTER_COUNT,
                     "DictionaryService returned an invalid letter set.");
        
        // Copy them over to the data set
        for (var x : int = 0; x < Properties.LETTERS; x++)
        {
            for (var y : int = 0; y < Properties.LETTERS; y++)
            {
                _model.updateBoardLetter (new Point (x, y), s [x * Properties.LETTERS + y]);
            }
        }
    }

    /** Takes a new letter from the UI, and checks it against game logic. */
    public function selectLetter (position : Point, name : String) : void
    {
        
    }

    
    // PRIVATE VARIABLES

    /** Game data interface */
    private var _model : Model;

}

}

