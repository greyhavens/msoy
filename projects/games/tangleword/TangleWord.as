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
public class TangleWord extends Sprite
    implements PropertyChangedListener, StateChangedListener, MessageReceivedListener
{

    // Constructor creates the board, and registers itself for events
    // and other startup information.
    public function TangleWord () : void
    {
        // Initialize game controller
        _gameCtrl = new EZGameControl (this);
        _gameCtrl.registerListener (this);

        _coordinator = new HostCoordinator (_gameCtrl);

        // Create MVC elements
        _controller = new Controller (null); // we'll set it later...
        _display = new Display (_controller);
        _model = new Model (_gameCtrl, _coordinator, _display);
        _controller.setModel (_model);       // ... as in, right here :)
        addChild (_display);

        // TODO: DEBUG
        _gameCtrl.localChat (_coordinator.amITheHost () ?
                             "I AM THE HOST! :)" :
                             "I'm not the host. :(");
        
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
            _gameCtrl.localChat ("Starting up!");
            if (_coordinator.amITheHost ())
            {
                initializeScoreboard ();
                initializeLetters ();
            }

            break;

        case StateChangedEvent.GAME_ENDED:
            _gameCtrl.localChat ("Done!");
            break;

        }
    }

    /** From PropertyChangedListener: deal with distributed game data changes */
    public function propertyChanged (event : PropertyChangedEvent) : void
    {
        // What kind of a message did we get?
        switch (event.name)
        {
        case SHARED_LETTER_SET:

            // We recieved a notification of a new shared letter set -
            // let's update the board
            Assert.True (event.newValue is Array, "Received invalid Shared Letter Set!");
            var s : Array = event.newValue as Array;
            if (s != null)
            {
                _controller.setGameBoard (s);
            }

            break;

        case SHARED_SCOREBOARD:

            // The scoreboard changed - let's change the pointer to remember
            // the new one instead.
            if (event.newValue != null)
            {
                _gameCtrl.localChat ("Got new score values...");
                _model.updateScoreboard (event.newValue);
            }

            break;
            

        default:
            Assert.Fail ("Unknown property changed: " + event.name);
        }
        
    }

    /** From MessageReceivedListener: deal with out-of-band messages */
    public function messageReceived (event : MessageReceivedEvent) : void
    {
        /* TODO */
        Assert.Fail ("MessageReceived");
    }


    // PRIVATE FUNCTIONS

    /** Calls the dictionary service to retrieve a new bag of letters,
        and distributes them among all peers.
    */
    private function initializeLetters () : void
    {
        // Get a set of letters
        var s : Array = DictionaryService.getLetterSet (LocaleSettings.EN_US,
                                                        Properties.LETTER_COUNT);
        Assert.True (s.length == Properties.LETTER_COUNT,
                     "DictionaryService returned an invalid letter set.");

        // Now pass it around via the distributed object
        _gameCtrl.set (SHARED_LETTER_SET, s);
    }

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
