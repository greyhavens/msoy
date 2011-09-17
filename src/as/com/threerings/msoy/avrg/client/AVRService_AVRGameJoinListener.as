//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.msoy.avrg.data.AVRGameConfig;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java AVRService_AVRGameJoinListener interface.
 */
public interface AVRService_AVRGameJoinListener
    extends InvocationService_InvocationListener
{
    // from Java AVRService_AVRGameJoinListener
    function avrgJoined (arg1 :int, arg2 :AVRGameConfig) :void
}
}
