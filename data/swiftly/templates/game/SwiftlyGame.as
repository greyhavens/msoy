//
// $Id$
//
// SwiftlyGame - a game for Whirled

package {

import flash.display.Sprite;

import flash.events.Event;

import com.whirled.game.GameControl;

[SWF(width="400", height="400")]
public class SwiftlyGame extends Sprite
{
    public function SwiftlyGame ()
    {
        // listen for an unload event
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        _control = new GameControl(this);
    }

    /**
     * This is called when your game is unloaded.
     */
    protected function handleUnload (event :Event) :void
    {
        // stop any sounds, clean up any resources that need it
    }

    protected var _control :GameControl;
}
}
