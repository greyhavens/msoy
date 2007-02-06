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

    /** Updates a single letter at specified /position/ to display a new /text/.  */
    public function updateBoardLetter (position : Point, text : String) : void
    {
        Assert.NotNull (_board, "Board needs to be initialized first.");
        _board[position.x][position.y] = text;
        _display.setText (position, text);
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

        // Third, list of players, which is just an associative array on an object
        _players = new Object ();
    }


    // PRIVATE VARIABLES

    /** Authoritative host coordinator */
    private var _coord : HostCoordinator;

    /** Main game control structure */
    private var _gameCtrl : EZGameControl;

    /** Game board data */
    private var _board : Array;

    /** Current word data (as array of letters) */
    private var _word : Array;
    
    /** Game board view */
    private var _display : Display;

    /** List of players and their scores */
    private var _players : Object;

        
}


}
