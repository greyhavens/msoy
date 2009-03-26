//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.client.MemberService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

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
    /** The method id used to dispatch {@link #acknowledgeWarning} requests. */
    public static final int ACKNOWLEDGE_WARNING = 1;

    // from interface MemberService
    public void acknowledgeWarning (Client arg1)
    {
        sendRequest(arg1, ACKNOWLEDGE_WARNING, new Object[] {});
    }

    /** The method id used to dispatch {@link #bootFromPlace} requests. */
    public static final int BOOT_FROM_PLACE = 2;

    // from interface MemberService
    public void bootFromPlace (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, BOOT_FROM_PLACE, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #complainMember} requests. */
    public static final int COMPLAIN_MEMBER = 3;

    // from interface MemberService
    public void complainMember (Client arg1, int arg2, String arg3)
    {
        sendRequest(arg1, COMPLAIN_MEMBER, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #ditchFollower} requests. */
    public static final int DITCH_FOLLOWER = 4;

    // from interface MemberService
    public void ditchFollower (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DITCH_FOLLOWER, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #followMember} requests. */
    public static final int FOLLOW_MEMBER = 5;

    // from interface MemberService
    public void followMember (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, FOLLOW_MEMBER, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getCurrentMemberLocation} requests. */
    public static final int GET_CURRENT_MEMBER_LOCATION = 6;

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
    public static final int GET_DISPLAY_NAME = 7;

    // from interface MemberService
    public void getDisplayName (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_DISPLAY_NAME, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getHomeId} requests. */
    public static final int GET_HOME_ID = 8;

    // from interface MemberService
    public void getHomeId (Client arg1, byte arg2, int arg3, InvocationService.ResultListener arg4)
    {
        InvocationMarshaller.ResultMarshaller listener4 = new InvocationMarshaller.ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, GET_HOME_ID, new Object[] {
            Byte.valueOf(arg2), Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #inviteAllToBeFriends} requests. */
    public static final int INVITE_ALL_TO_BE_FRIENDS = 9;

    // from interface MemberService
    public void inviteAllToBeFriends (Client arg1, int[] arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_ALL_TO_BE_FRIENDS, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #inviteToBeFriend} requests. */
    public static final int INVITE_TO_BE_FRIEND = 10;

    // from interface MemberService
    public void inviteToBeFriend (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_TO_BE_FRIEND, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #inviteToFollow} requests. */
    public static final int INVITE_TO_FOLLOW = 11;

    // from interface MemberService
    public void inviteToFollow (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_TO_FOLLOW, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #setAvatar} requests. */
    public static final int SET_AVATAR = 12;

    // from interface MemberService
    public void setAvatar (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, SET_AVATAR, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #setAway} requests. */
    public static final int SET_AWAY = 13;

    // from interface MemberService
    public void setAway (Client arg1, boolean arg2, String arg3)
    {
        sendRequest(arg1, SET_AWAY, new Object[] {
            Boolean.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #setDisplayName} requests. */
    public static final int SET_DISPLAY_NAME = 14;

    // from interface MemberService
    public void setDisplayName (Client arg1, String arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, SET_DISPLAY_NAME, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #setHomeSceneId} requests. */
    public static final int SET_HOME_SCENE_ID = 15;

    // from interface MemberService
    public void setHomeSceneId (Client arg1, int arg2, int arg3, int arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_HOME_SCENE_ID, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), Integer.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #setMuted} requests. */
    public static final int SET_MUTED = 16;

    // from interface MemberService
    public void setMuted (Client arg1, int arg2, boolean arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_MUTED, new Object[] {
            Integer.valueOf(arg2), Boolean.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #updateAvailability} requests. */
    public static final int UPDATE_AVAILABILITY = 17;

    // from interface MemberService
    public void updateAvailability (Client arg1, int arg2)
    {
        sendRequest(arg1, UPDATE_AVAILABILITY, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #updateStatus} requests. */
    public static final int UPDATE_STATUS = 18;

    // from interface MemberService
    public void updateStatus (Client arg1, String arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_STATUS, new Object[] {
            arg2, listener3
        });
    }
}
