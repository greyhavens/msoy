//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;

/**
 * An ActionScript version of the Java PetService interface.
 */
public interface PetService extends InvocationService
{
    // from Java interface PetService
    function callPet (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface PetService
    function orderPet (arg1 :Client, arg2 :int, arg3 :int, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface PetService
    function sendChat (arg1 :Client, arg2 :int, arg3 :int, arg4 :String, arg5 :InvocationService_ConfirmListener) :void;
}
}
