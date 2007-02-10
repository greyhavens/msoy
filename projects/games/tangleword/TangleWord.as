package
{

import flash.display.Sprite;    
import flash.display.Shape;

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;



/**
   Main game takes care of initializing network connections,
   maintaining distributed data representation, and responding to events.
*/

[SWF(width="500", height="500")]
public class TangleWord extends Sprite implements StateChangedListener
{
    
    // PUBLIC METHODS

    // Constructor creates the board, and registers itself for events
    // and other startup information.
    public function TangleWord () : void
    {
        // Initialize game data
        _gameCtrl = new EZGameControl (this);
        _gameCtrl.registerListener (this);
        _coordinator = new HostCoordinator (_gameCtrl);
        _rounds = new RoundProvider (_gameCtrl, _coordinator);

        _rounds.setTimeout (RoundProvider.SYSTEM_STARTED_STATE, 0);
        _rounds.setTimeout (RoundProvider.ROUND_STARTED_STATE, Properties.ROUND_LENGTH);
        _rounds.setTimeout (RoundProvider.ROUND_ENDED_STATE, Properties.PAUSE_LENGTH);
        

        // Create MVC elements
        _controller = new Controller (null, _rounds); // we'll set it later...
        _display = new Display (_controller, _rounds);
        _model = new Model (_gameCtrl, _coordinator, _rounds, _display);
        _controller.setModel (_model);       // ... as in, right here :)
        addChild (_display);

        // TODO: DEBUG
        // _gameCtrl.localChat (_coordinator.amITheHost () ?
        //                     "I AM THE HOST! :)" :
        //                     "I'm not the host. :(");

        if (_gameCtrl.isInPlay()) {
            checkStartup();
        }
    }


    //
    //
    // EVENT HANDLERS

    /** From StateChangedListener: deal with the game starting and ending. */
    public function stateChanged (event : StateChangedEvent) : void
    {
        switch (event.type)
        {
        case StateChangedEvent.GAME_STARTED:
            checkStartup();
            break;

        case StateChangedEvent.GAME_ENDED:
            _gameCtrl.localChat ("Done!");
            break;

        }
    }

    /**
     * Check to see if we should be starting the game.
     */
    protected function checkStartup () :void
    {
        _gameCtrl.localChat("Starting up!");
        if (_coordinator.amITheHost()) {
            initializeScoreboard();
        }
    }

    // PRIVATE FUNCTIONS

    /** Creates a new distributed scoreboard */
    private function initializeScoreboard () : void
    {
        // Create a new instance, and fill in the names
        var board : Scoreboard = new Scoreboard ();
        var names : Array = _gameCtrl.getPlayerNames ();
        for (var name : String in names)
        {
            board.addPlayer (name);
        }

        // Finally, share it!
        _gameCtrl.set (SHARED_SCOREBOARD, board.internalScoreObject);
    }
        
    


    // PRIVATE MEMBER VARIABLES

    /** Game control object */
    private var _gameCtrl : EZGameControl;

    /** Helps us determine who is the authoritative host */
    private var _coordinator : HostCoordinator;

    /** Coordinates round info */
    private var _rounds : RoundProvider;
    
    /** Data interface */
    private var _model : Model;

    /** Data display */
    private var _display : Display;

    /** Data validation */
    private var _controller : Controller;


    // PRIVATE CONSTANTS

    /** Key name: shared letter set */
    private static const SHARED_LETTER_SET : String = "Shared Letter Set";

    /** Key name: shared scoreboard */
    private static const SHARED_SCOREBOARD : String = "Shared Scoreboard";

    
}
    
    
    
}
