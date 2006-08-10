package com.threerings.msoy.game.data {

import com.threerings.presents.dobj.ChangeListener;

/**
 * A listener that will be notified of PropertySet events.
 */
public interface PropertySetListener extends ChangeListener
{
    /**
     * Called when a property was set on a flash game object.
     * This will be called <em>after</em> the event has been applied
     * to the object.
     */
    function propertyWasSet (event :PropertySetEvent) :void;
}
}
