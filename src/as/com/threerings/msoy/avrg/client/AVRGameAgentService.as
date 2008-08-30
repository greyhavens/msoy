//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * An ActionScript version of the Java AVRGameAgentService interface.
 */
public interface AVRGameAgentService extends InvocationService
{
    // from Java interface AVRGameAgentService
    function leaveGame (arg1 :Client, arg2 :int) :void;
}
}
