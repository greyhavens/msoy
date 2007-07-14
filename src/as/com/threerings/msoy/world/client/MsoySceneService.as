//
// $Id$

package com.threerings.msoy.world.client {

import flash.utils.ByteArray;
import com.threerings.io.TypedArray;
import com.threerings.msoy.world.client.MsoySceneService;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.whirled.client.SceneService_SceneMoveListener;
import com.threerings.whirled.data.SceneMarshaller_SceneMoveMarshaller;

/**
 * An ActionScript version of the Java MsoySceneService interface.
 */
public interface MsoySceneService extends InvocationService
{
    // from Java interface MsoySceneService
    function moveTo (arg1 :Client, arg2 :int, arg3 :int, arg4 :MsoyLocation, arg5 :SceneService_SceneMoveListener) :void;
}
}
