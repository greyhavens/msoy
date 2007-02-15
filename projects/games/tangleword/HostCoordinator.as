package
{

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.OccupantChangedEvent;
import com.threerings.ezgame.OccupantChangedListener;

/**
   HostCoordinator provides methods for determining whether the current
   client should consider itself the authority in dealing with game state
   (for example, when dealing out cards, or setting up the game board).

   Current implementation is very simple: the oldest client in the game
   is automatically authoritative. However, since clients can enter and leave,
   any client can potentially become the host at any time.

   Usage example:
   private var _coord : HostCoordinator = new HostCoordinator (_gameCtrl);
   ...
   function xxx ()
   {
   if (_coord.amITheHost ())
     {
       dealCards ();
     } 
   }
       
*/

public class HostCoordinator implements OccupantChangedListener
{
    /**
       Constructor, expects an initialized instance of EZGameControl.
       If the optional showDebug flag is set, it will display host info
       as chat messages (only useful for debugging).
     */
    public function HostCoordinator (
        control : EZGameControl, showDebug : Boolean = false)
    {
        Assert.NotNull (control, "HostCoordinator initialized with a null control!");
            
        _control = control;
        _control.registerListener (this);

        _showDebug = showDebug;

        tryClaimHostRole ();
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
        // If the occupant who just left was the host, everyone should try
        // and clear their role. This means that everyone will clear
        // and reclaim the role - so only the last one will actually succeed.
        // It's a little redundant, but fine. :)
        if (getHostId () == event.occupantId)
        {
            if (_showDebug) { _control.localChat ("Removing the old host..."); }
            _control.set (HOST_NAME, null);
        }
        tryClaimHostRole ();
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
        if (! hostExists())
        {
            // Only set host role if it's not already claimed (hence: testAndSet)
            _control.testAndSet (HOST_NAME, _control.getMyId ());
        }
        debugHostStatus ();
    }

    /** Debug only */
    private function debugHostStatus () : void
    {
        if (_showDebug)
        {
            _control.localChat (getHostId() == _control.getMyId() ?
                                "I am the host." :
                                "I'm not the host.");
        }
    }


    // PRIVATE VALUES

    /** Magic key that stores current authoritative host */
    private var HOST_NAME : String = "HOST_COORDINATOR_AUTHORITATIVE_HOST";

    /** Controller storage */
    private var _control : EZGameControl = null;

    /** Debug flag */
    private var _showDebug : Boolean;
}

}


