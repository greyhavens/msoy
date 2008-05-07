//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.msoy.data.all.JabberName;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java JabberService interface.
 */
public interface JabberService extends InvocationService
{
    // from Java interface JabberService
    function registerIM (arg1 :Client, arg2 :String, arg3 :String, arg4 :String, arg5 :InvocationService_InvocationListener) :void;

    // from Java interface JabberService
    function sendMessage (arg1 :Client, arg2 :JabberName, arg3 :String, arg4 :InvocationService_ResultListener) :void;

    // from Java interface JabberService
    function unregisterIM (arg1 :Client, arg2 :String, arg3 :InvocationService_InvocationListener) :void;
}
}
