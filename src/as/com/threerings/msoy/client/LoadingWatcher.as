//
// $Id$

package com.threerings.msoy.client {

import flash.display.LoaderInfo;
import flash.events.IEventDispatcher;

public interface LoadingWatcher
{
    /**
     * Called to hand us a tasty loaderinfo in which to sink our tendrils.
     *
     * @param info the primary loaderInfo to watch.
     * @param unloadie an extra EventDispatcher on which to receive UNLOAD events,
     *   since a LoaderInfo will never dispatch UNLOAD until an INIT has been dispatched,
     *   and we need to be able to track cancelled loads.
     * @param isPrimary true if this LoaderInfo represents the "primary" media.
     */
    function watchLoader (
        info :LoaderInfo, unloadie :IEventDispatcher, isPrimary :Boolean = false) :void;
}
}
