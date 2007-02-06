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
    implements PropertyChangedListener, StateChangedListener
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

        // for testing only - TODO: move out to synchronized section
        _controller.randomizeGameBoard ();
        
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
            break;

        case StateChangedEvent.GAME_ENDED:
            _gameCtrl.localChat ("Done!");
            break;

        }
    }

    /** From PropertyChangedListener: deal with distributed game data changes */
    public function propertyChanged (event : PropertyChangedEvent) : void
    {
        /* TODO */
        Assert.Fail ("PropertyChanged");
    }


    // PRIVATE FUNCTIONS

    


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
}
    
    
    
}
