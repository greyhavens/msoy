//
// $Id$

package com.threerings.msoy.bureau.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java ThaneWorldService interface.
 */
public interface ThaneWorldService extends InvocationService
{
    // from Java interface ThaneWorldService
    function locateRoom (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;
}
}
