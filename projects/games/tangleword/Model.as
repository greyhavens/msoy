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

public class Model
{

    // PUBLIC METHODS
    
    public function Model (gameCtrl : EZGameControl,
                           coordinator : HostCoordinator,
                           display : Display) : void
    {
        // Squirrel the pointers away
        _coord = coordinator;
        _display = display;
        _gameCtrl = gameCtrl;

        // Initialize game data storage
        initializeStorage ();
    }


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

    
                

    /** Updates a single letter at specified /position/ to display a new /text/.  */
    public function updateBoardLetter (position : Point, text : String) : void
    {
        Assert.NotNull (_board, "Board needs to be initialized first.");
        _board[position.x][position.y] = text;
        _display.setLetter (position, text);
    }

    /** Updates the scoreboard. Unfortunately it's a little heavy-handed,
        replacing the entire board data structure. This needs revisiting. :) */
    public function updateScoreboard (scores : Object) : void
    {
        _scoreboard.internalScoreObject = scores;
    }



    // PRIVATE METHODS

    /** Resets the currently guessed word */
    public function resetWord () : void
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


    // PRIVATE VARIABLES

    /** Authoritative host coordinator */
    private var _coord : HostCoordinator;

    /** Main game control structure */
    private var _gameCtrl : EZGameControl;

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
