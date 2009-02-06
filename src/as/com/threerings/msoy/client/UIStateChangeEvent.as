//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;

/**
 * Event dispatched when a value changes in the <code>UIState</code>.
 */
public class UIStateChangeEvent extends Event
{
    /** Type constant for ui state change events. */
    public static const STATE_CHANGE :String = "msoy.UIStateChange";

    /** Creates a new ui state change event. */
    public function UIStateChangeEvent ()
    {
        super(STATE_CHANGE);
    }
}

}
