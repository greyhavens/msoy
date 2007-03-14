package
{

import com.whirled.WhirledGameControl;

import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.geom.Point;


/** The Controller class holds game logic, and updates game state in the model. */
    
public class Controller 
{
    // OUBLIC CONSTANTS
    public static const MIN_WORD_LENGTH : int = 3;

    
    // PUBLIC METHODS
    
    public function Controller (
        gameCtrl : WhirledGameControl, model : Model, rounds : RoundProvider) : void
    {
        _gameCtrl = gameCtrl;
        _model = model;
        _rounds = rounds;
        _rounds.addEventListener (RoundProviderEvent.STARTED, roundStartedHandler);
        _rounds.addEventListener (RoundProviderEvent.ENDED, roundEndedHandler);
    }

    /** Shutdown handler */
    public function handleUnload (event : Event) : void
    {
        _rounds.removeEventListener (RoundProviderEvent.STARTED, roundStartedHandler);
        _rounds.removeEventListener (RoundProviderEvent.ENDED, roundEndedHandler);
    }

    /** Update model that's being controlled. */
    public function setModel (model : Model) : void
    {
        _model = model;
    }

    /** Returns true if the controller should accept player inputs, false otherwise */
    public function get enabled () : Boolean
    {
        return _enabled;
    }

    /** Sets the value specifying whether the controller should accept player inputs */
    public function set enabled (value : Boolean) : void
    {
        _enabled = value;
    }
    
    /** Takes a new letter from the UI, and checks it against game logic. */
    public function tryAddLetter (position : Point) : void
    {
        if (enabled)
        {
            // Position of the letter on top of the stack 
            var lastLetterPosition : Point = _model.getLastLetterPosition ();
            
            // Did the player click on the first letter? If so, clear out
            // the current word field, and add it.
            var noPreviousLetterFound : Boolean = (lastLetterPosition == null);
            if (noPreviousLetterFound)
            {
                _model.removeAllSelectedLetters (); 
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
            // If so, add it.
            var isValidNeighbor : Boolean = (areNeighbors (position, lastLetterPosition) &&
                                             ! _model.isLetterSelectedAtPosition (position));
            if (isValidNeighbor)
            {
                _model.selectLetterAtPosition (position);
                return;
            }
            
            // Player clicked on an invalid position - don't do anything
        }
    }


    /** Signals that the currently selected word is a candidate for scoring.
        It will be matched against the dictionary, and added to the model. */
    public function tryScoreWord (word : String, typed : Boolean) : void
    {
        // This is the callback that gets called after the word is successfully
        // checked against the dictionary
        var success : Function = function (word : String, isvalid : Boolean) : void
        {
            // If this word was typed in, check if it exists on the board
            if (typed) {
                isvalid = isvalid && _model.wordExistsOnBoard (word.toLowerCase());
            }
            
            // Finally, process the new word. Notice that we don't check if it's already
            // been claimed - the model will take care of that, because there's a network
            // round-trip involved, and therefore potential of contention.
            var score : Number = word.length;
            _model.addScore (word, score, isvalid);
        }
        
        // First, check to make sure it's of the correct length (in characters)
        if (word.length < MIN_WORD_LENGTH) return;

        // Normalize the word
        word = word.toLowerCase ();

        // Now check if it's an actual word.
        _gameCtrl.checkDictionaryWord (Properties.LOCALE, word, success);

    }
            
    /**
       Called when the user types a letter inside the word field.
    */
    public function processKeystroke (event : KeyboardEvent) : void
    {
        // The user typed in some character. Typing is incompatible
        // with mouse selection, so if there's already anything selected
        // by clicking, clear it all, and start afresh.
        if (_model.getLastLetterPosition () != null)
        {
            _model.removeAllSelectedLetters ();
        }
    }
    


    // EVENT HANDLERS

    /** Called when the round starts - enables user input, randomizes data. */
    private function roundStartedHandler (event : RoundProviderEvent) : void
    {
        initializeLetterSet ();
        enabled = true;
    }

    /** Called when the round ends - disables user input. */
    private function roundEndedHandler (event : RoundProviderEvent) : void
    {
        enabled = false;
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
    
    /** If this client is the host, initializes a new letter set. */
    private function initializeLetterSet () : void
    {
        if (_gameCtrl.amInControl ())
        {
            var success : Function = function (a : Array) : void
            {
                _model.sendNewLetterSet (a);
            }
            _gameCtrl.getDictionaryLetterSet (Properties.LOCALE,
                                              Properties.LETTER_COUNT,
                                              success);
        }
    }


    
    // PRIVATE VARIABLES

    /** Game helper */
    private var _gameCtrl : WhirledGameControl;
    
    /** Game data interface */
    private var _model : Model;

    /** Round provider */
    private var _rounds : RoundProvider;

    /** Does the controller accept user input? */
    private var _enabled : Boolean;
}

}

