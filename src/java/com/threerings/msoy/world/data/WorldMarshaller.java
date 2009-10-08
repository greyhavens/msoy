//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.world.client.WorldService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link WorldService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class WorldMarshaller extends InvocationMarshaller
    implements WorldService
{
    /**
     * Marshalls results to implementations of {@link WorldService.HomeResultListener}.
     */
    public static class HomeResultMarshaller extends ListenerMarshaller
        implements HomeResultListener
    {
        /** The method id used to dispatch {@link #readyToEnter}
         * responses. */
        public static final int READY_TO_ENTER = 1;

        // from interface HomeResultMarshaller
        public void readyToEnter (int arg1)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, READY_TO_ENTER,
                               new Object[] { Integer.valueOf(arg1) }, transport));
        }

        /** The method id used to dispatch {@link #selectGift}
         * responses. */
        public static final int SELECT_GIFT = 2;

        // from interface HomeResultMarshaller
        public void selectGift (Avatar[] arg1, int arg2)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, SELECT_GIFT,
                               new Object[] { arg1, Integer.valueOf(arg2) }, transport));
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case READY_TO_ENTER:
                ((HomeResultListener)listener).readyToEnter(
                    ((Integer)args[0]).intValue());
                return;

            case SELECT_GIFT:
                ((HomeResultListener)listener).selectGift(
                    (Avatar[])args[0], ((Integer)args[1]).intValue());
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #acceptAndProceed} requests. */
    public static final int ACCEPT_AND_PROCEED = 1;

    // from interface WorldService
    public void acceptAndProceed (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ACCEPT_AND_PROCEED, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #ditchFollower} requests. */
    public static final int DITCH_FOLLOWER = 2;

    // from interface WorldService
    public void ditchFollower (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DITCH_FOLLOWER, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #followMember} requests. */
    public static final int FOLLOW_MEMBER = 3;

    // from interface WorldService
    public void followMember (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, FOLLOW_MEMBER, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getHomeId} requests. */
    public static final int GET_HOME_ID = 4;

    // from interface WorldService
    public void getHomeId (Client arg1, byte arg2, int arg3, WorldService.HomeResultListener arg4)
    {
        WorldMarshaller.HomeResultMarshaller listener4 = new WorldMarshaller.HomeResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, GET_HOME_ID, new Object[] {
            Byte.valueOf(arg2), Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #getHomePageGridItems} requests. */
    public static final int GET_HOME_PAGE_GRID_ITEMS = 5;

    // from interface WorldService
    public void getHomePageGridItems (Client arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_HOME_PAGE_GRID_ITEMS, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #inviteToFollow} requests. */
    public static final int INVITE_TO_FOLLOW = 6;

    // from interface WorldService
    public void inviteToFollow (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_TO_FOLLOW, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #setAvatar} requests. */
    public static final int SET_AVATAR = 7;

    // from interface WorldService
    public void setAvatar (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, SET_AVATAR, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #setHomeSceneId} requests. */
    public static final int SET_HOME_SCENE_ID = 8;

    // from interface WorldService
    public void setHomeSceneId (Client arg1, int arg2, int arg3, int arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_HOME_SCENE_ID, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), Integer.valueOf(arg4), listener5
        });
    }
}
