//
// $Id$

package com.threerings.msoy.data {

import com.threerings.msoy.client.MemberService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;
import com.threerings.util.Name;

import com.threerings.util.Integer;
import com.threerings.util.langBoolean;

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
    /** The method id used to dispatch {@link #alterBuddy} requests. */
    public static const ALTER_FRIEND :int = 1;

    // documentation inherited from interface
    public function alterFriend (arg1 :Client, arg2 :int, arg3 :Boolean, arg4 :InvocationService_InvocationListener) :void
    {
        var listener4 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, ALTER_FRIEND, [
            Integer.valueOf(arg2), langBoolean.valueOf(arg3), listener4
        ]);
    }

}
}
