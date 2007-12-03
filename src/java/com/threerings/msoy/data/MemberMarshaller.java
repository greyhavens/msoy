//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link MemberService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MemberMarshaller extends InvocationMarshaller
    implements MemberService
{
    /** The method id used to dispatch {@link #acknowledgeNotifications} requests. */
    public static final int ACKNOWLEDGE_NOTIFICATIONS = 1;

    // from interface MemberService
    public void acknowledgeNotifications (Client arg1, int[] arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ACKNOWLEDGE_NOTIFICATIONS, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #followMember} requests. */
    public static final int FOLLOW_MEMBER = 2;

    // from interface MemberService
    public void followMember (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, FOLLOW_MEMBER, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getCurrentMemberLocation} requests. */
    public static final int GET_CURRENT_MEMBER_LOCATION = 3;

    // from interface MemberService
    public void getCurrentMemberLocation (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_CURRENT_MEMBER_LOCATION, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getDisplayName} requests. */
    public static final int GET_DISPLAY_NAME = 4;

    // from interface MemberService
    public void getDisplayName (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_DISPLAY_NAME, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getGroupName} requests. */
    public static final int GET_GROUP_NAME = 5;

    // from interface MemberService
    public void getGroupName (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_GROUP_NAME, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getHomeId} requests. */
    public static final int GET_HOME_ID = 6;

    // from interface MemberService
    public void getHomeId (Client arg1, byte arg2, int arg3, InvocationService.ResultListener arg4)
    {
        InvocationMarshaller.ResultMarshaller listener4 = new InvocationMarshaller.ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, GET_HOME_ID, new Object[] {
            Byte.valueOf(arg2), Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #inviteToBeFriend} requests. */
    public static final int INVITE_TO_BE_FRIEND = 7;

    // from interface MemberService
    public void inviteToBeFriend (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_TO_BE_FRIEND, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #inviteToFollow} requests. */
    public static final int INVITE_TO_FOLLOW = 8;

    // from interface MemberService
    public void inviteToFollow (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_TO_FOLLOW, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #issueInvitation} requests. */
    public static final int ISSUE_INVITATION = 9;

    // from interface MemberService
    public void issueInvitation (Client arg1, MemberName arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ISSUE_INVITATION, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #setAvatar} requests. */
    public static final int SET_AVATAR = 10;

    // from interface MemberService
    public void setAvatar (Client arg1, int arg2, float arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_AVATAR, new Object[] {
            Integer.valueOf(arg2), Float.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #setDisplayName} requests. */
    public static final int SET_DISPLAY_NAME = 11;

    // from interface MemberService
    public void setDisplayName (Client arg1, String arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, SET_DISPLAY_NAME, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #setHomeSceneId} requests. */
    public static final int SET_HOME_SCENE_ID = 12;

    // from interface MemberService
    public void setHomeSceneId (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, SET_HOME_SCENE_ID, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #updateAvailability} requests. */
    public static final int UPDATE_AVAILABILITY = 13;

    // from interface MemberService
    public void updateAvailability (Client arg1, int arg2)
    {
        sendRequest(arg1, UPDATE_AVAILABILITY, new Object[] {
            Integer.valueOf(arg2)
        });
    }
}
