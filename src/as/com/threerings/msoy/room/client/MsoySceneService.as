//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.whirled.client.SceneService_SceneMoveListener;

/**
 * An ActionScript version of the Java MsoySceneService interface.
 */
public interface MsoySceneService extends InvocationService
{
    // from Java interface MsoySceneService
    function moveTo (arg1 :Client, arg2 :int, arg3 :int, arg4 :int, arg5 :MsoyLocation, arg6 :SceneService_SceneMoveListener) :void;
}
}
