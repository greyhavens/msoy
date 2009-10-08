//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.io.TypedArray;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.whirled.data.SceneModel;

/**
 * An ActionScript version of the Java MsoySceneService_MsoySceneMoveListener interface.
 */
public interface MsoySceneService_MsoySceneMoveListener
    extends InvocationService_InvocationListener
{
    // from Java MsoySceneService_MsoySceneMoveListener
    function moveRequiresServerSwitch (arg1 :String, arg2 :TypedArray /* of int */) :void

    // from Java MsoySceneService_MsoySceneMoveListener
    function moveSucceeded (arg1 :int, arg2 :PlaceConfig) :void

    // from Java MsoySceneService_MsoySceneMoveListener
    function moveSucceededWithScene (arg1 :int, arg2 :PlaceConfig, arg3 :SceneModel) :void

    // from Java MsoySceneService_MsoySceneMoveListener
    function moveSucceededWithUpdates (arg1 :int, arg2 :PlaceConfig, arg3 :TypedArray /* of class com.threerings.whirled.data.SceneUpdate */) :void

    // from Java MsoySceneService_MsoySceneMoveListener
    function moveToBeHandledByAVRG (arg1 :int, arg2 :int) :void

    // from Java MsoySceneService_MsoySceneMoveListener
    function selectGift (arg1 :TypedArray /* of class com.threerings.msoy.item.data.all.Avatar */) :void
}
}
