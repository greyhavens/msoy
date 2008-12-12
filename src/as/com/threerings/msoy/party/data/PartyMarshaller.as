//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.msoy.party.client.PartyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.util.Byte;
import com.threerings.util.Integer;
import com.threerings.util.langBoolean;

/**
 * Provides the implementation of the <code>PartyService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PartyMarshaller extends InvocationMarshaller
    implements PartyService
{
    /** The method id used to dispatch <code>assignLeader</code> requests. */
    public static const ASSIGN_LEADER :int = 1;

    // from interface PartyService
    public function assignLeader (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ASSIGN_LEADER, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>bootMember</code> requests. */
    public static const BOOT_MEMBER :int = 2;

    // from interface PartyService
    public function bootMember (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, BOOT_MEMBER, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>inviteMember</code> requests. */
    public static const INVITE_MEMBER :int = 3;

    // from interface PartyService
    public function inviteMember (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_MEMBER, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>leaveParty</code> requests. */
    public static const LEAVE_PARTY :int = 4;

    // from interface PartyService
    public function leaveParty (arg1 :Client, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, LEAVE_PARTY, [
            listener2
        ]);
    }

    /** The method id used to dispatch <code>updateNameOrStatus</code> requests. */
    public static const UPDATE_NAME_OR_STATUS :int = 5;

    // from interface PartyService
    public function updateNameOrStatus (arg1 :Client, arg2 :String, arg3 :Boolean, arg4 :InvocationService_InvocationListener) :void
    {
        var listener4 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, UPDATE_NAME_OR_STATUS, [
            arg2, langBoolean.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch <code>updateRecruitment</code> requests. */
    public static const UPDATE_RECRUITMENT :int = 6;

    // from interface PartyService
    public function updateRecruitment (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_RECRUITMENT, [
            Byte.valueOf(arg2), listener3
        ]);
    }
}
}
