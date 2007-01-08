//
// $Id$

package com.threerings.msoy.export {

import flash.display.DisplayObject;
import flash.events.Event;
import flash.events.TimerEvent;
import flash.utils.Timer;

/**
 * Defines actions, accessors and callbacks available to all Pets.
 */
public class PetControl extends EntityControl
{
    /**
     * A function that is called periodically (twice a second by default) to allow the Pet to do
     * its "thinking", make any desired changes to its memory, wander around the room, trigger
     * different animation events and generally execute its behavior. This is where all AI code
     * should run, rather than every frame (where only animation related code should run), to
     * ensure that coordination is properly handled when multiple clients are viewing the same Pet.
     */
    public var tick :Function;

    /**
     * Creates a controller for a Pet. The display object is the Pet's visualization.
     */
    public function PetControl (disp :DisplayObject)
    {
        super(disp);
    }

    /**
     * Configures the interval on which this Pet is "ticked" in milliseconds. The default is twice
     * a second (500ms). The tick interval should be no smaller than 100ms to avoid bogging down
     * the client.
     */
    public function setTickInterval (interval :Number) :void
    {
        _tickInterval = (interval > 100) ? interval : 100;
        if (_ticker != null) {
            _ticker.delay = _tickInterval;
        }
    }

    // from MsoyControl
    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);

        // TODO
    }

    // from MsoyControl
    override protected function handleUnload (evt :Event) :void
    {
        super.handleUnload(evt);

        if (_ticker != null) {
            _ticker.stop();
            _ticker = null;
        }
    }

    /**
     * Called when this client has been assigned control of this pet.
     */
    protected function clientReceivedControl_v1 () :void
    {
        _ticker = new Timer(_tickInterval, 0);
        _ticker.addEventListener(TimerEvent.TIMER, function (evt :TimerEvent) :void {
            if (tick != null) {
                tick();
            }
        });
        _ticker.start();
    }

    /**
     * Called when this client has lost control of this pet. TODO: do we even need this?
     */
    protected function clientLostControl_v1 () :void
    {
        if (_ticker != null) {
            _ticker.stop();
            _ticker = null;
        }
    }

    /** Our desired tick interval (in milliseconds). */
    protected var _tickInterval :Number = 500;

    /** Used to tick this Pet when this client is running its AI. */
    protected var _ticker :Timer;
}
}
