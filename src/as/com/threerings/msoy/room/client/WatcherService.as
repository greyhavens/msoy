//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * An ActionScript version of the Java WatcherService interface.
 */
public interface WatcherService extends InvocationService
{
    // from Java interface WatcherService
    function addWatch (arg1 :Client, arg2 :int) :void;

    // from Java interface WatcherService
    function clearWatch (arg1 :Client, arg2 :int) :void;
}
}
