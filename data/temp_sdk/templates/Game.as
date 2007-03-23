//
// $Id$
//
// @project@ - a game for Whirled

package {

import flash.display.Sprite;

import flash.events.Event;

import com.whirled.WhirledGameControl;

[SWF(width="400", height="400")]
public class @project@ extends Sprite
{
    public function @project@ ()
    {
        // listen for an unload event
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        _control = new WhirledGameControl(this);
    }

    /**
     * This is called when your game is unloaded.
     */
    protected function handleUnload (event :Event) :void
    {
        // stop any sounds, clean up any resources that need it.  This specifically includes 
        // unregistering listeners to any events - especially Event.ENTER_FRAME
    }

    protected var _control :WhirledGameControl;
}
}
