package
{


import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

import flash.geom.Point;

/**
   GameModel is a game-specific interface to the networked data set.
   It contains accessors to get the list of players, scores, etc.
*/

public class Model implements MessageReceivedListener, PropertyChangedListener
{

    // PUBLIC METHODS
    
    public function Model (gameCtrl : EZGameControl,
                           coordinator : HostCoordinator,
                           rounds : RoundProvider,
                           display : Display) : void
    {
        // Squirrel the pointers away
        _gameCtrl = gameCtrl;
        _coord = coordinator;
        _rounds = rounds;
        _display = display;
        _playerName = _gameCtrl.getPlayerNames()[_gameCtrl.getMyIndex()];

        // Register for updates
        _gameCtrl.registerListener (this);
        _rounds.addEventListener (RoundProvider.ROUND_ENDED_STATE, roundEndedHandler);

        // Initialize game data storage
        initializeStorage ();
    }


    //
    //
    // LETTER ACCESSORS
    
    /** If this board letter is already selected as part of the word, returns true.  */
    public function isLetterSelectedAtPosition (position : Point) : Boolean
    {
        var pointMatches : Function =
            function (item : Point, index : int, array : Array) : Boolean
            {
                return (item.equals (position));
            };

        return _word.some (pointMatches);
    }

    /** Returns coordinates of the most recently added word, or null. */
    public function getLastLetterPosition () : Point
    {
        if (_word.length > 0)
        {
            return _word[_word.length - 1] as Point;
        }

        return null;
    }

    /** Adds a new letter to the word (by adding a pair of coordinates) */
    public function selectLetterAtPosition (position : Point) : void
    {
        _word.push (position);
        _display.updateLetterSelection (_word);
    }

    /** Removes last selected letter from the word (if applicable) */
    public function removeLastSelectedLetter () : void
    {
        if (_word.length > 0)
        {
            _word.pop ();
            _display.updateLetterSelection (_word);
        }
    }

    /** Removes all selected letters, resetting the word. */
    public function removeAllSelectedLetters () : void
    {
        _word = new Array ();
        _display.updateLetterSelection (_word);
    }
    




    //
    //
    // SHARED DATA ACCESSORS

    /** Sends out a message to everyone, informing them about adding
        the new word to their lists. */
    public function addScore (word : String, score : Number) : void
    {
        var obj : Object = new Object ();
        obj.player = _playerName;
        obj.word = word;
        obj.score = score;

        _gameCtrl.sendMessage (ADD_SCORE_MSG, obj);
    }

    /** Sends out a message to everyone, informing them about a new letter set.
        The array contains strings corresponding to the individual letters. */
    public function sendNewLetterSet (a : Array) : void
    {
        _gameCtrl.set (LETTER_SET_MSG, a);
    }
        
        


    //
    //
    // EVENT HANDLERS

    /** From MessageReceivedListener: checks for special messages signaling
        game data updates. */
    public function messageReceived (event : MessageReceivedEvent) : void
    {
        switch (event.name)
        {
        case ADD_SCORE_MSG:
            
            // store the score
            addWordToScoreboard (event.value.player,
                                 event.value.word,
                                 event.value.score);

            // reset selection
            removeAllSelectedLetters ();
            updateScoreDisplay ();
            
            break;

        default:
            // Ignore any other messages; they're not for us.

        }

    }
    
    /** From PropertyChangedListener: deal with distributed game data changes */
    public function propertyChanged (event : PropertyChangedEvent) : void
    {
        // What kind of a message did we get?
        switch (event.name)
        {
        case LETTER_SET_MSG:

            // We recieved a notification of a new shared letter set -
            // let's update the board
            Assert.True (event.newValue is Array, "Received invalid Shared Letter Set!");
            var s : Array = event.newValue as Array;
            if (s != null)
            {
                setGameBoard (s);
            }

            break;

        default:
            Assert.Fail ("Unknown property changed: " + event.name);
        }
        
    }

    /** Called when the round ends - cleans up data. */
    public function roundEndedHandler (newState : String) : void
    {
        removeAllSelectedLetters ();
    }

    

    //
    //
    // PRIVATE METHODS

    /** Resets the currently guessed word */
    private function resetWord () : void
    {
        _word = new Array ();
    }
         

    /** Initializes letter and word storage */
    public function initializeStorage () : void
    {
        // First, the board
        _board = new Array (Properties.LETTERS);
        for (var x : int = 0; x < _board.length; x++)
        {
            _board[x] = new Array (Properties.LETTERS);
            for (var y : int = 0; y < _board[x].length; y++)
            {
                _board[x][y] = "!";
            }
        }

        // Second, the currently assembled word
        resetWord ();

        // Third, make a new scoreboard
        _scoreboard = new Scoreboard ();
    }

    /** Sets up a new game board, based on a flat array of letters. */
    public function setGameBoard (s : Array) : void
    {
        // Copy them over to the data set
        for (var x : int = 0; x < Properties.LETTERS; x++)
        {
            for (var y : int = 0; y < Properties.LETTERS; y++)
            {
                updateBoardLetter (new Point (x, y), s [x * Properties.LETTERS + y]);
            }
        }
    }

    /** Checks if the word is not in the scoreboard already, and if it isn't, adds it. */
    private function addWordToScoreboard (player : String, word : String, score : Number) : void
    {
        if (_scoreboard.getWordOwner (word) == null)
        {
            _scoreboard.addWord (player, word, score);
            _display.logSuccess (player, word, score);
        }
        else
        {
            if (_playerName == player)
            {
                _display.logFailure (player, word);
            }
        }
    }      
    
    /** Updates a single letter at specified /position/ to display a new /text/.  */
    private function updateBoardLetter (position : Point, text : String) : void
    {
        Assert.NotNull (_board, "Board needs to be initialized first.");
        _board[position.x][position.y] = text;
        _display.setLetter (position, text);
    }

    /** Updates the total scores displayed on the board */
    private function updateScoreDisplay () : void
    {
        _display.updateScores (_scoreboard);
    }


    

    // PRIVATE CONSTANTS

    /** Message types */
    private static const ADD_SCORE_MSG : String = "Score Update";
    private static const LETTER_SET_MSG : String = "Letter Set Update";

    
    // PRIVATE VARIABLES

    /** Authoritative host coordinator */
    private var _coord : HostCoordinator;

    /** Main game control structure */
    private var _gameCtrl : EZGameControl;

    /** Round provider */
    private var _rounds : RoundProvider;
    
    /** Cache the player's name */
    private var _playerName : String;

    /** Game board data */
    private var _board : Array;

    /** Current word data (as array of board coordinates) */
    private var _word : Array;
    
    /** Game board view */
    private var _display : Display;

    /** List of players and their scores */
    private var _scoreboard : Scoreboard;

}


}



