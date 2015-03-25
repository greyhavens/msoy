//
// $Id$

package com.threerings.msoy.party.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.party.client.PartyService;

/**
 * Provides the implementation of the {@link PartyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from PartyService.java.")
public class PartyMarshaller extends InvocationMarshaller<ClientObject>
    implements PartyService
{
    /** The method id used to dispatch {@link #assignLeader} requests. */
    public static final int ASSIGN_LEADER = 1;

    // from interface PartyService
    public void assignLeader (int arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(ASSIGN_LEADER, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #bootMember} requests. */
    public static final int BOOT_MEMBER = 2;

    // from interface PartyService
    public void bootMember (int arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(BOOT_MEMBER, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #inviteMember} requests. */
    public static final int INVITE_MEMBER = 3;

    // from interface PartyService
    public void inviteMember (int arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(INVITE_MEMBER, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #moveParty} requests. */
    public static final int MOVE_PARTY = 4;

    // from interface PartyService
    public void moveParty (int arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(MOVE_PARTY, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #setGame} requests. */
    public static final int SET_GAME = 5;

    // from interface PartyService
    public void setGame (int arg1, byte arg2, int arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(SET_GAME, new Object[] {
            Integer.valueOf(arg1), Byte.valueOf(arg2), Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #updateDisband} requests. */
    public static final int UPDATE_DISBAND = 6;

    // from interface PartyService
    public void updateDisband (boolean arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(UPDATE_DISBAND, new Object[] {
            Boolean.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #updateRecruitment} requests. */
    public static final int UPDATE_RECRUITMENT = 7;

    // from interface PartyService
    public void updateRecruitment (byte arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(UPDATE_RECRUITMENT, new Object[] {
            Byte.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #updateStatus} requests. */
    public static final int UPDATE_STATUS = 8;

    // from interface PartyService
    public void updateStatus (String arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(UPDATE_STATUS, new Object[] {
            arg1, listener2
        });
    }
}
