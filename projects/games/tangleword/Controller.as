package
{


import flash.geom.Point;


/** The Controller class holds game logic, and updates game state in the model. */
    
public class Controller 
{
    // OUBLIC CONSTANTS
    public static const MIN_WORD_LENGTH : int = 3;

    
    // PUBLIC METHODS
    
    public function Controller (model : Model) : void
    {
        _model = model;

    }

    public function setModel (model : Model) : void
    {
        _model = model;
    }


    /** Initializes a new letter set. */
    public function initializeLetterSet () : void
    {
        // Get a set of letters
        var s : Array = DictionaryService.getLetterSet (TangleWord.LOCALE,
                                                        Properties.LETTER_COUNT);
        
        Assert.True (s.length == Properties.LETTER_COUNT,
                     "DictionaryService returned an invalid letter set.");

        _model.sendNewLetterSet (s);
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


    /** Signals that the currently selected word is a candidate for scoring.
        It will be matched against the dictionary, and added to the model. */
    public function tryScoreWord (word : String) : void
    {
        // First, check to make sure it's of the correct length (in characters)
        if (word.length < MIN_WORD_LENGTH) return;

        // Check if it's already been claimed
        // TODO
        
        // Now check if it's an actual word
        if (!DictionaryService.checkWord (TangleWord.LOCALE, word)) return;

        // Find the word score
        // TODO
        var score : Number = 10;
        
        // Finally, process the new word
        _model.addScore (word, score);
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

