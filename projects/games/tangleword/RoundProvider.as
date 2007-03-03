package
{

import com.threerings.ezgame.HostCoordinator;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.msoy.export.WhirledGameControl;
import flash.utils.Timer;
import flash.events.TimerEvent;


/**
   RoundProvider takes care of starting and ending rounds based on
   a timer, or manual state changes. Rounds can be diagrammed
   as the following state space:

   [ Stopped ] ==> [ Started ] ==> [ Round Started ] <==> [ Round Ended ]

   Each state can have an associated timeout, after which the system
   will transition to the next state. Timeouts are set using setTimeout ():
     _provider.setTimeout (ROUND_STARTED_STATE, 60);
     
   Every state transition can result in callbacks; to receive them,
   the client needs to register using addEventListener(), for example:
     _provider.addEventListener (ROUND_ENDED_STATE, scoringHandler);

   System state can also be set manually, by calling setCurrentState (), e.g.
     _provider.setCurrentState (RoundProvider.ROUND_STARTED_STATE);
   However, only the authoritative host will succeed in changing the state.     
*/

public class RoundProvider implements PropertyChangedListener
{
    /** These constants describe system states. */
    public static const SYSTEM_STOPPED_STATE : String = "System Stopped";
    public static const SYSTEM_STARTED_STATE : String = "System Started";
    public static const ROUND_STARTED_STATE  : String = "Round Started";
    public static const ROUND_ENDED_STATE    : String = "Round Ended";

    public static const STATE_LIST : Array =
        [ SYSTEM_STOPPED_STATE,
          SYSTEM_STARTED_STATE,
          ROUND_STARTED_STATE,
          ROUND_ENDED_STATE ];

    
    /**
       Constructor. Expects an instance of WhirledGameControl.
       A newly-created instance of a round provider is not active,
       and needs to be started with a call to one of the
       initialize...() functions.
    */
    public function RoundProvider (
        gameCtrl : WhirledGameControl,
        coord : HostCoordinator)
    {
        // Store the pointers
        _coord = coord;
        _gameCtrl = gameCtrl;
        _gameCtrl.registerListener (this);

        // Initialize
        initializeTables ();
        setCurrentState (SYSTEM_STOPPED_STATE);
    }

    /**
       Sets a timeout for the specified state. Transition out of this state
       will happen after the number of seconds specified in /timeout/.
       
       Parameters:
         state - constant string, one of RoundProvider.*_STATE
         seconds - timeout value in seconds
    */
    public function setTimeout (state : String, seconds : Number) : void
    {
        // Initialize the timer, if necessary
        if (_timer == null)
        {
            _timer = new Timer (200, 0); // updates every few hundred ms
            _timer.addEventListener (TimerEvent.TIMER, timerHandler);
            _timer.start ();
        }

        // Now set the timeout
        _timeouts[state] = seconds * 1000; // convert to milliseconds
    }
        
    
    /**
       Sets the current state to the specified value, along with the
       appropriate timeout. Returns true if the client was an authoritative
       host and succeeded in setting the state; false otherwise.

       Parameters:
         newState - constant string, one of RoundProvider.*_STATE.
    */
    public function setCurrentState (newState : String) : Boolean
    {
        Assert.NotNull (newState, "Trying to set state to null.");
        Assert.True (checkStateName (newState), "Trying to set invalid state: " + newState);
        if (checkStateName (newState))
        {
            // Only the host will actually succeed doing this...
            if (_coord.status == HostCoordinator.STATUS_HOST)
            {
                var newStateTimeout : Number = (new Date()).time + _timeouts[newState];
                _gameCtrl.set (ROUND_PROVIDER_CURRENT_STATE_PROPERTY, newState);
                _gameCtrl.set (ROUND_PROVIDER_CURRENT_STATE_TIMEOUT, newStateTimeout);
                return true;
            }
        }

        return false;
    }

    /** Retrieves current state */
    public function getCurrentState () : String
    {
        return (_gameCtrl.get (ROUND_PROVIDER_CURRENT_STATE_PROPERTY) as String);
    }

    /** Retrieves the desired end-time for the current state, in the same format
        as returned from Date.time (i.e. milliseconds since the beginnning of
        the Unix epoch). If the current state does not have a defined timeout,
        it will return a NaN.

        Note: this timeout value may be a few hundred milliseconds off from what
        is used to trigger events, and should be used for UI/display purposes only. 
    */
    public function getCurrentStateTimeout () : Number
    {
        return (_gameCtrl.get (ROUND_PROVIDER_CURRENT_STATE_TIMEOUT) as Number);
    }

    /** Adds the specified function to the list of state change callbacks;
        it will be called when the specified /newState/ is entered.
        
        The listener function is required to have the following signature:
          function (newState : String) : void

        Please note that the listener function will only be called once,
        at the transition into the new state.
    */
    public function addEventListener (newState : String, listener : Function) : void
    {
        if (checkStateName (newState))
        {
            var listeners : Array = _stateListeners[newState];
            listeners.push (listener);
        }
    }

    /** Removes the listener function. */
    public function removeEventListener (state : String, listener : Function) : void
    {
        if (checkStateName (state))
        {
            var listeners : Array = _stateListeners[state];
            var i : int = listeners.indexOf (listener);
            if (i > -1)
            {
                listeners.splice (i, 1);
            }
        }
    }


    // EVENT HANDLERS

    /**
       From PropertySetListener: when the authoritative host changed
       the current state, this event should be propagated to any of our
       listeners.
    */
    public function propertyChanged (event : PropertyChangedEvent) : void
    {
        switch (event.name)
        {
        case ROUND_PROVIDER_CURRENT_STATE_PROPERTY:
        
            // Pull out state name
            var state : String = event.newValue as String;
            var valid : Boolean = checkStateName (state);
            Assert.True (valid, "Received a set state message, but the state is invalid!");

            if (valid)
            {
                // Remember the time when we entered this state
                var now : Date = new Date();
                _timestamps[state] = now.time;

                // Now call any listeners
                var listeners : Array = _stateListeners[state];
                Assert.NotNull (listeners, "Listeners array for " + state + " doesn't exist!");

                for each (var fn : Function in listeners)
                {
                    fn.call (null, state);
                }
            }
            break;

        case ROUND_PROVIDER_CURRENT_STATE_TIMEOUT:
        
            // _gameCtrl.localChat ("Current state timeout: " + event.newValue);
            break;


        }
    }          

    /**
       Processes ticks from the timer. These arrive every few hundred ms,
       and we use them to determine whether to time out of the current state.

       Please note: all clients receive timer events, but only the
       authoritative host will be able to actually change the state.
    */
    public function timerHandler (event : TimerEvent) : void
    {
        // Do we even have a timeout defined for this state?
        var currentState : String = getCurrentState ();
        if (_timeouts[currentState] != undefined)
        {
            // Okay, let's get the last timestamp
            var timeout : Number = _timeouts[currentState] as Number;
            var timestamp : Number = _timestamps[currentState] as Number;
            
            var now : Date = new Date();
            var deltams : Number = (now.time - timestamp);

            if (deltams >= timeout)
            {
                // We're done here; try to transition to a new state
                var nextState : String = _nextState [currentState];
                setCurrentState (nextState);
            }
        }
    }
            


    // PRIVATE FUNCTIONS

    /** Initializes the state transition and message tables */
    private function initializeTables () : void
    {
        // State transitions
        _nextState [SYSTEM_STOPPED_STATE] = SYSTEM_STARTED_STATE;
        _nextState [SYSTEM_STARTED_STATE] = ROUND_STARTED_STATE;
        _nextState [ROUND_STARTED_STATE]  = ROUND_ENDED_STATE;
        _nextState [ROUND_ENDED_STATE]    = ROUND_STARTED_STATE;

        // State listeners
        for each (var state : String in STATE_LIST)
        {
            _stateListeners[state] = new Array ();
        }
    }

    /** Checks if the state name is valid */
    private function checkStateName (stateName : String) : Boolean
    {
        return (STATE_LIST.indexOf (stateName) > -1);
    }

    /**
       For authoritative hosts only:
       Sets the shared variable to represent timeout for the current state,
       in milliseconds since the beginning of the Unix epoch
    */
    private function setCurrentStateTimeout (value : Number) : Boolean
    {
        // Only the host will actually succeed doing this...
        if (_coord.status == HostCoordinator.STATUS_HOST)
        {
            _gameCtrl.set (ROUND_PROVIDER_CURRENT_STATE_TIMEOUT, value);
            return true;
        }
        
        return false;
    }


    // PRIVATE CONSTANTS

    /** Variable that holds current state */
    private static const ROUND_PROVIDER_CURRENT_STATE_PROPERTY : String =
        "_round_current_state";

    /** Variable that holds the current state timeout */
    private static const ROUND_PROVIDER_CURRENT_STATE_TIMEOUT : String =
        "_round_current_timeout";
       
    
    
    // PRIVATE VARIABLES

    /** Local EZ control storage */
    private var _gameCtrl : WhirledGameControl;

    /** Local host coordinator storage */
    private var _coord : HostCoordinator;

    /** Timeout timer. If this variable is null, it means no timer
        was initialized; otherwise it's set to the instance that will
        call our callback function. */
    private var _timer : Timer;


    /** Private timeout object: an associative list mapping state names
        to timeout values (in milliseconds). If the state doesn't exist
        as a key, it means there is no timeout defined for that state. */
    private var _timeouts : Object = new Object ();

    /** Private timestamp object: an associative list mapping state names
        to the time when the state was last entered (in milliseconds since
        the beginning of the Unix epoch). If the state doesn't exist
        as a key, it means it has not been visited yet. */
    private var _timestamps : Object = new Object ();

    /** State transition table */
    private var _nextState : Object = new Object ();

    /** State to message table */
    private var _stateListeners : Object = new Object ();
}




}
