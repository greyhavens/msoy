package
{

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.OccupantChangedEvent;
import com.threerings.ezgame.OccupantChangedListener;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.PropertyChangedEvent;

/**
   HostCoordinator provides methods for determining whether the current
   client should consider itself the authority in dealing with game state
   (for example, when dealing out cards, or setting up the game board).

   The constructor takes two arguments: an EZ game controller, and
   a function that will be called once host coordination is established.
   This function can be used to start up the game.
   
   Usage example:

   private var _coord : HostCoordinator =
     new HostCoordinator (_gameCtrl, beginGame);

   ...

   function beginGame ()
   {
   if (_coord.amITheHost ())
     {
       dealCards ();
     } 
   }
       
*/

public class HostCoordinator
    implements OccupantChangedListener, PropertyChangedListener
{
    /**
       Constructor, expects an initialized instance of EZGameControl.
         
       If the optional showDebug flag is set, it will display host info
       as chat messages (only useful for debugging).

       Make sure to call join() when ready to start the game.
     */
    public function HostCoordinator (control : EZGameControl, showDebug : Boolean = false)
    {
        Assert.NotNull (control, "HostCoordinator initialized with a null control!");
            
        _control = control;
        _control.registerListener (this);

        _showDebug = showDebug;

        _hostKnown = false;
    }

    /**
       This function initiates host coordination, which may result
       in the current client becoming the authoritative game host,
       or just another player. :)

       It takes an optional callback function of type:
         function () : void { }

       The callback function will be called once host coordination was
       established, whether or not this client became the host.
    */
    public function join (callback : Function = null) : void
    {
        _startFn = callback;
        if (hostExists ()) {
            dealWithHostObservation ();
        } else {
            tryClaimHostRole ();
        }
    }


    /**
       Main query function: returns true if the current client
       is the authoritative host for the game, false otherwise.
           
       Note: do not save the results of calling this function.
       The value can change: any player could become the authoritative
       host at any moment in the game, since other players may
       leave and join the game at will. This is especially true when
       the authoritative host drops out of the game; in this case
       one of the other players will suddenly become authoritative.
    */
    public function amITheHost () : Boolean
    {
        debugHostStatus ();
        return (getHostId () == _control.getMyId ());
    }



    // EVENT HANDLERS

    /** Keep track of people coming in */
    public function occupantEntered (event : OccupantChangedEvent) : void
    {
        debugHostStatus ();
    }

    /** Keep track of people leaving */
    public function occupantLeft (event : OccupantChangedEvent) : void
    {
        // If the occupant who just left was the host, every client will
        // get a shot at trying to clear their role.
        if (_showDebug) {
            _control.localChat ("My ID: " + _control.getMyId ());
            _control.localChat ("Occupant left: " + event.occupantId);
            _control.localChat ("Current host: " + getHostId());
        }
        if (getHostId () == event.occupantId)
        {
            if (_showDebug) { _control.localChat ("Removing the old host..."); }
            // Clear the old host value, and put myself in their place.
            // Only the first client will succeed in doing this.
            _control.testAndSet (HOST_NAME, _control.getMyId(), event.occupantId);
            debugHostStatus ();
        }
    }
    
    /** Keep track of changing host values */
    public function propertyChanged (event : PropertyChangedEvent) : void
    {
        if (event.name == HOST_NAME && ! _hostKnown)
        {
            dealWithHostObservation ();
        }
    }    
    

    // PRIVATE FUNCTIONS

    /** Does the host exist? (Note: may be inaccurate due to roundtrip delay) */
    private function hostExists () : Boolean
    {
        return (_control.get (HOST_NAME) != null);
    }
    
    /** Get the current host ID (will be -1 if there is no authoritative host) */
    private function getHostId () : Number
    {
        var hostvalue : Object = _control.get (HOST_NAME);
        if (hostvalue != null && hostvalue is Number)
        {
            return hostvalue as Number;
        }
        return -1;
    }

    /** If the host role is unclaimed, claim it! */
    private function tryClaimHostRole () : void
    {
        if (_showDebug) {
            _control.localChat ("Trying to claim host role.");
            _control.localChat ("Does host exist? " + hostExists());
        }
            
        if (! hostExists())
        {
            // Only set host role if it's not already claimed (i.e. null)
            _control.testAndSet (HOST_NAME, _control.getMyId (), null);
        }
    }

    /** When we get a mess */
    private function dealWithHostObservation () : void
    {
        // The first time we notice a host, remember it, and
        // call the listener function, if applicable
        if (hostExists ())
        {
            if (_showDebug) { _control.localChat ("First host observation!"); }
            _hostKnown = true;
            if (_startFn != null)
            {
                _startFn();
            }
        }
    }
    

    /** Debug only */
    private function debugHostStatus () : void
    {
        if (_showDebug)
        {
            _control.localChat (
                (getHostId() == _control.getMyId() ? "I am" : "I'm not") +
                " the host (id " + getHostId() + ", mine is " + _control.getMyId() + ")");
        }
    }


    // PRIVATE VALUES

    /** Magic key that stores current authoritative host */
    private var HOST_NAME : String = "HOST_COORDINATOR_AUTHORITATIVE_HOST";

    /** Controller storage */
    private var _control : EZGameControl = null;

    /** Debug flag */
    private var _showDebug : Boolean;

    /** Have we seen at least one host message? */
    private var _hostKnown : Boolean;
    
    /** Startup function, to be called once the host was decided */
    private var _startFn : Function;
}

}


