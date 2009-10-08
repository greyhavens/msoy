//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.io.TypedArray;
import com.threerings.msoy.room.client.MsoySceneService_MsoySceneMoveListener;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.whirled.data.SceneModel;

/**
 * Marshalls instances of the MsoySceneService_MsoySceneMoveMarshaller interface.
 */
public class MsoySceneMarshaller_MsoySceneMoveMarshaller
    extends InvocationMarshaller_ListenerMarshaller
{
    /** The method id used to dispatch <code>moveRequiresServerSwitch</code> responses. */
    public static const MOVE_REQUIRES_SERVER_SWITCH :int = 1;

    /** The method id used to dispatch <code>moveSucceeded</code> responses. */
    public static const MOVE_SUCCEEDED :int = 2;

    /** The method id used to dispatch <code>moveSucceededWithScene</code> responses. */
    public static const MOVE_SUCCEEDED_WITH_SCENE :int = 3;

    /** The method id used to dispatch <code>moveSucceededWithUpdates</code> responses. */
    public static const MOVE_SUCCEEDED_WITH_UPDATES :int = 4;

    /** The method id used to dispatch <code>moveToBeHandledByAVRG</code> responses. */
    public static const MOVE_TO_BE_HANDLED_BY_AVRG :int = 5;

    /** The method id used to dispatch <code>selectGift</code> responses. */
    public static const SELECT_GIFT :int = 6;

    // from InvocationMarshaller_ListenerMarshaller
    override public function dispatchResponse (methodId :int, args :Array) :void
    {
        switch (methodId) {
        case MOVE_REQUIRES_SERVER_SWITCH:
            (listener as MsoySceneService_MsoySceneMoveListener).moveRequiresServerSwitch(
                (args[0] as String), (args[1] as TypedArray /* of int */));
            return;

        case MOVE_SUCCEEDED:
            (listener as MsoySceneService_MsoySceneMoveListener).moveSucceeded(
                (args[0] as int), (args[1] as PlaceConfig));
            return;

        case MOVE_SUCCEEDED_WITH_SCENE:
            (listener as MsoySceneService_MsoySceneMoveListener).moveSucceededWithScene(
                (args[0] as int), (args[1] as PlaceConfig), (args[2] as SceneModel));
            return;

        case MOVE_SUCCEEDED_WITH_UPDATES:
            (listener as MsoySceneService_MsoySceneMoveListener).moveSucceededWithUpdates(
                (args[0] as int), (args[1] as PlaceConfig), (args[2] as TypedArray /* of class com.threerings.whirled.data.SceneUpdate */));
            return;

        case MOVE_TO_BE_HANDLED_BY_AVRG:
            (listener as MsoySceneService_MsoySceneMoveListener).moveToBeHandledByAVRG(
                (args[0] as int), (args[1] as int));
            return;

        case SELECT_GIFT:
            (listener as MsoySceneService_MsoySceneMoveListener).selectGift(
                (args[0] as TypedArray /* of class com.threerings.msoy.item.data.all.Avatar */));
            return;

        default:
            super.dispatchResponse(methodId, args);
            return;
        }
    }
}
}
