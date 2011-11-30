//
// $Id$

package com.threerings.msoy.world.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.world.client.WorldService;

/**
 * Provides the implementation of the {@link WorldService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from WorldService.java.")
public class WorldMarshaller extends InvocationMarshaller
    implements WorldService
{
    /**
     * Marshalls results to implementations of {@code WorldService.HomeResultListener}.
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
            sendResponse(READY_TO_ENTER, new Object[] { Integer.valueOf(arg1) });
        }

        /** The method id used to dispatch {@link #selectGift}
         * responses. */
        public static final int SELECT_GIFT = 2;

        // from interface HomeResultMarshaller
        public void selectGift (Avatar[] arg1, int arg2)
        {
            sendResponse(SELECT_GIFT, new Object[] { arg1, Integer.valueOf(arg2) });
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
    public void acceptAndProceed (int arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(ACCEPT_AND_PROCEED, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #completeDjTutorial} requests. */
    public static final int COMPLETE_DJ_TUTORIAL = 2;

    // from interface WorldService
    public void completeDjTutorial (InvocationService.InvocationListener arg1)
    {
        ListenerMarshaller listener1 = new ListenerMarshaller();
        listener1.listener = arg1;
        sendRequest(COMPLETE_DJ_TUTORIAL, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #ditchFollower} requests. */
    public static final int DITCH_FOLLOWER = 3;

    // from interface WorldService
    public void ditchFollower (int arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(DITCH_FOLLOWER, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #followMember} requests. */
    public static final int FOLLOW_MEMBER = 4;

    // from interface WorldService
    public void followMember (int arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(FOLLOW_MEMBER, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #getHomeId} requests. */
    public static final int GET_HOME_ID = 5;

    // from interface WorldService
    public void getHomeId (byte arg1, int arg2, WorldService.HomeResultListener arg3)
    {
        WorldMarshaller.HomeResultMarshaller listener3 = new WorldMarshaller.HomeResultMarshaller();
        listener3.listener = arg3;
        sendRequest(GET_HOME_ID, new Object[] {
            Byte.valueOf(arg1), Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getHomePageGridItems} requests. */
    public static final int GET_HOME_PAGE_GRID_ITEMS = 6;

    // from interface WorldService
    public void getHomePageGridItems (InvocationService.ResultListener arg1)
    {
        InvocationMarshaller.ResultMarshaller listener1 = new InvocationMarshaller.ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(GET_HOME_PAGE_GRID_ITEMS, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #inviteToFollow} requests. */
    public static final int INVITE_TO_FOLLOW = 7;

    // from interface WorldService
    public void inviteToFollow (int arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(INVITE_TO_FOLLOW, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #setAvatar} requests. */
    public static final int SET_AVATAR = 8;

    // from interface WorldService
    public void setAvatar (int arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(SET_AVATAR, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #setHomeSceneId} requests. */
    public static final int SET_HOME_SCENE_ID = 9;

    // from interface WorldService
    public void setHomeSceneId (int arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(SET_HOME_SCENE_ID, new Object[] {
            Integer.valueOf(arg1), Integer.valueOf(arg2), Integer.valueOf(arg3), listener4
        });
    }
}
