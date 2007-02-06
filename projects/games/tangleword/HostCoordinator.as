package
{

import com.threerings.ezgame.EZGameControl;

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

public class HostCoordinator
{
    /** Constructor, expects an initialized instance of EZGameControl */
    public function HostCoordinator (control : EZGameControl)
    {
        Assert.NotNull (control, "HostCoordinator was initialized with a null!");
            
        _control = control;
    }

    /**
       Main query function: returns true if the current client
       is the authoritative host for the game, false otherwise.
           
       Note: do not save the results of calling this function.
       The value can change: any player could become the authoritative
       host at any moment in the game, since other players may
       leave and join the game at will.
    */
    public function amITheHost () : Boolean
    {
        // In this simple implementation, we make authoritative
        // the first player on the player list 
        return (_control.getMyIndex () == 0);
    }


    // PRIVATE VALUES

    /** Controller storage */
    private var _control : EZGameControl = null;
}

}


