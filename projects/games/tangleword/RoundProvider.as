package
{

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;

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

public class RoundProvider implements MessageReceivedListener
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
       Constructor. Expects an instance of EZGameControl.
       A newly-created instance of a round provider is not active,
       and needs to be started with a call to one of the
       initialize...() functions.
    */
    public function RoundProvider (
        gameCtrl : EZGameControl,
        hostCoord : HostCoordinator)
    {
        // Store the pointers
        _hostCoord = hostCoord;
        _gameCtrl = gameCtrl;
        _gameCtrl.registerListener (this);

        // Initialize
        initializeTables ();
        setCurrentState (SYSTEM_STARTED_STATE);
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
       Sets the current state to the specified value.
       Returns true if the client was an authoritative host and succeeded
       in setting the state; false otherwise.

       Parameters:
         newState - constant string, one of RoundProvider.*_STATE.
    */
    public function setCurrentState (newState : String) : Boolean
    {
        Assert.NotNull (newState, "Trying to set state to null.");
        Assert.True (checkStateName (newState), "Trying to set invalid state: " + newState);
        if (checkStateName (newState))
        {
            // Only the host can do this...
            if (_hostCoord.amITheHost ())
            {
                // The host now sends out the message that a new state was set,
                // and every client RoundProvider will get this message and
                // update is current state accordingly. 
                _gameCtrl.sendMessage (ROUND_PROVIDER_SET_STATE_MESSAGE,
                                       newState);

                return true;
            }
        }

        return false;
    }

    /** Retrieves current state */
    public function getCurrentState () : String
    {
        return _currentState;
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
       From MessageReceivedListener: checks for messages from the
       authoritative host, signaling state change.
    */
    public function messageReceived (event : MessageReceivedEvent) : void
    {
        if (event.name == ROUND_PROVIDER_SET_STATE_MESSAGE)
        {
            // Pull out state name
            var state : String = event.value as String;
            var valid : Boolean = checkStateName (state);
            Assert.True (valid, "Received a set state message, but the state is invalid!");

            if (valid)
            {
                // First, set the state locally
                _currentState = state;

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
        }
    }          

    /**
       Processes ticks from the timer. These arrive every second, and we use them
       to determine whether to time out of the current state.

       Please note: all clients receive timer events, but only the
       authoritative host will be able to actually change the state.
    */
    public function timerHandler (event : TimerEvent) : void
    {
        // Do we even have a timeout defined for this state?
        if (_timeouts[_currentState] != undefined)
        {
            // Okay, let's get the last timestamp
            var timeout : Number = _timeouts[_currentState] as Number;
            var timestamp : Number = _timestamps[_currentState] as Number;
            
            var now : Date = new Date();
            var deltams : Number = (now.time - timestamp);

            if (deltams >= timeout)
            {
                // We're done here; let's transition to a new state
                var nextState : String = _nextState [_currentState];
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


    // PRIVATE CONSTANTS

    /** This is the message sent from the authoritative host to everyone else,
        to signal state transition. */
    private static const ROUND_PROVIDER_SET_STATE_MESSAGE : String =
        "ROUND_PROVIDER_SET_STATE_MESSAGE";
       
    
    
    // PRIVATE VARIABLES

    /** Local EZ control storage */
    private var _gameCtrl : EZGameControl;

    /** Local host coordinator storage */
    private var _hostCoord : HostCoordinator;

    /** Current state */
    private var _currentState : String;

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
