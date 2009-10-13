//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.room.client.MsoySceneService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;
import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

/**
 * Provides the implementation of the {@link MsoySceneService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MsoySceneMarshaller extends InvocationMarshaller
    implements MsoySceneService
{
    /**
     * Marshalls results to implementations of {@link MsoySceneService.MsoySceneMoveListener}.
     */
    public static class MsoySceneMoveMarshaller extends ListenerMarshaller
        implements MsoySceneMoveListener
    {
        /** The method id used to dispatch {@link #moveRequiresServerSwitch}
         * responses. */
        public static final int MOVE_REQUIRES_SERVER_SWITCH = 1;

        // from interface MsoySceneMoveMarshaller
        public void moveRequiresServerSwitch (String arg1, int[] arg2)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, MOVE_REQUIRES_SERVER_SWITCH,
                               new Object[] { arg1, arg2 }, transport));
        }

        /** The method id used to dispatch {@link #moveSucceeded}
         * responses. */
        public static final int MOVE_SUCCEEDED = 2;

        // from interface MsoySceneMoveMarshaller
        public void moveSucceeded (int arg1, PlaceConfig arg2)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, MOVE_SUCCEEDED,
                               new Object[] { Integer.valueOf(arg1), arg2 }, transport));
        }

        /** The method id used to dispatch {@link #moveSucceededWithScene}
         * responses. */
        public static final int MOVE_SUCCEEDED_WITH_SCENE = 3;

        // from interface MsoySceneMoveMarshaller
        public void moveSucceededWithScene (int arg1, PlaceConfig arg2, SceneModel arg3)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, MOVE_SUCCEEDED_WITH_SCENE,
                               new Object[] { Integer.valueOf(arg1), arg2, arg3 }, transport));
        }

        /** The method id used to dispatch {@link #moveSucceededWithUpdates}
         * responses. */
        public static final int MOVE_SUCCEEDED_WITH_UPDATES = 4;

        // from interface MsoySceneMoveMarshaller
        public void moveSucceededWithUpdates (int arg1, PlaceConfig arg2, SceneUpdate[] arg3)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, MOVE_SUCCEEDED_WITH_UPDATES,
                               new Object[] { Integer.valueOf(arg1), arg2, arg3 }, transport));
        }

        /** The method id used to dispatch {@link #moveToBeHandledByAVRG}
         * responses. */
        public static final int MOVE_TO_BE_HANDLED_BY_AVRG = 5;

        // from interface MsoySceneMoveMarshaller
        public void moveToBeHandledByAVRG (int arg1, int arg2)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, MOVE_TO_BE_HANDLED_BY_AVRG,
                               new Object[] { Integer.valueOf(arg1), Integer.valueOf(arg2) }, transport));
        }

        /** The method id used to dispatch {@link #selectGift}
         * responses. */
        public static final int SELECT_GIFT = 6;

        // from interface MsoySceneMoveMarshaller
        public void selectGift (Avatar[] arg1, String arg2)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, SELECT_GIFT,
                               new Object[] { arg1, arg2 }, transport));
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case MOVE_REQUIRES_SERVER_SWITCH:
                ((MsoySceneMoveListener)listener).moveRequiresServerSwitch(
                    (String)args[0], (int[])args[1]);
                return;

            case MOVE_SUCCEEDED:
                ((MsoySceneMoveListener)listener).moveSucceeded(
                    ((Integer)args[0]).intValue(), (PlaceConfig)args[1]);
                return;

            case MOVE_SUCCEEDED_WITH_SCENE:
                ((MsoySceneMoveListener)listener).moveSucceededWithScene(
                    ((Integer)args[0]).intValue(), (PlaceConfig)args[1], (SceneModel)args[2]);
                return;

            case MOVE_SUCCEEDED_WITH_UPDATES:
                ((MsoySceneMoveListener)listener).moveSucceededWithUpdates(
                    ((Integer)args[0]).intValue(), (PlaceConfig)args[1], (SceneUpdate[])args[2]);
                return;

            case MOVE_TO_BE_HANDLED_BY_AVRG:
                ((MsoySceneMoveListener)listener).moveToBeHandledByAVRG(
                    ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue());
                return;

            case SELECT_GIFT:
                ((MsoySceneMoveListener)listener).selectGift(
                    (Avatar[])args[0], (String)args[1]);
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #moveTo} requests. */
    public static final int MOVE_TO = 1;

    // from interface MsoySceneService
    public void moveTo (Client arg1, int arg2, int arg3, int arg4, MsoyLocation arg5, MsoySceneService.MsoySceneMoveListener arg6)
    {
        MsoySceneMarshaller.MsoySceneMoveMarshaller listener6 = new MsoySceneMarshaller.MsoySceneMoveMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, MOVE_TO, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), Integer.valueOf(arg4), arg5, listener6
        });
    }
}
