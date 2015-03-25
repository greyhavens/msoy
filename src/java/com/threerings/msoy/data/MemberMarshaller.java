//
// $Id$

package com.threerings.msoy.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.client.MemberService;

/**
 * Provides the implementation of the {@link MemberService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from MemberService.java.")
public class MemberMarshaller extends InvocationMarshaller<ClientObject>
    implements MemberService
{
    /** The method id used to dispatch {@link #acknowledgeWarning} requests. */
    public static final int ACKNOWLEDGE_WARNING = 1;

    // from interface MemberService
    public void acknowledgeWarning ()
    {
        sendRequest(ACKNOWLEDGE_WARNING, new Object[] {
        });
    }

    /** The method id used to dispatch {@link #bootFromPlace} requests. */
    public static final int BOOT_FROM_PLACE = 2;

    // from interface MemberService
    public void bootFromPlace (int arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(BOOT_FROM_PLACE, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #complainMember} requests. */
    public static final int COMPLAIN_MEMBER = 3;

    // from interface MemberService
    public void complainMember (int arg1, String arg2)
    {
        sendRequest(COMPLAIN_MEMBER, new Object[] {
            Integer.valueOf(arg1), arg2
        });
    }

    /** The method id used to dispatch {@link #getCurrentMemberLocation} requests. */
    public static final int GET_CURRENT_MEMBER_LOCATION = 4;

    // from interface MemberService
    public void getCurrentMemberLocation (int arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_CURRENT_MEMBER_LOCATION, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #getDisplayName} requests. */
    public static final int GET_DISPLAY_NAME = 5;

    // from interface MemberService
    public void getDisplayName (int arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_DISPLAY_NAME, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #inviteAllToBeFriends} requests. */
    public static final int INVITE_ALL_TO_BE_FRIENDS = 6;

    // from interface MemberService
    public void inviteAllToBeFriends (int[] arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(INVITE_ALL_TO_BE_FRIENDS, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #inviteToBeFriend} requests. */
    public static final int INVITE_TO_BE_FRIEND = 7;

    // from interface MemberService
    public void inviteToBeFriend (int arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(INVITE_TO_BE_FRIEND, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #setAway} requests. */
    public static final int SET_AWAY = 8;

    // from interface MemberService
    public void setAway (String arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(SET_AWAY, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #setDisplayName} requests. */
    public static final int SET_DISPLAY_NAME = 9;

    // from interface MemberService
    public void setDisplayName (String arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(SET_DISPLAY_NAME, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #setMuted} requests. */
    public static final int SET_MUTED = 10;

    // from interface MemberService
    public void setMuted (int arg1, boolean arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(SET_MUTED, new Object[] {
            Integer.valueOf(arg1), Boolean.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #updateStatus} requests. */
    public static final int UPDATE_STATUS = 11;

    // from interface MemberService
    public void updateStatus (String arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(UPDATE_STATUS, new Object[] {
            arg1, listener2
        });
    }
}
