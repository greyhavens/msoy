//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * An ActionScript version of the Java MsoySceneService interface.
 */
public interface MsoySceneService extends InvocationService
{
    // from Java interface MsoySceneService
    function moveTo (arg1 :int, arg2 :int, arg3 :int, arg4 :MsoyLocation, arg5 :MsoySceneService_MsoySceneMoveListener) :void;
}
}
