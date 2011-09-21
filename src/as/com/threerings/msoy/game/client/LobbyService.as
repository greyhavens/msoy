//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java LobbyService interface.
 */
public interface LobbyService extends InvocationService
{
    // from Java interface LobbyService
    function identifyLobby (arg1 :int, arg2 :InvocationService_ResultListener) :void;

    // from Java interface LobbyService
    function playNow (arg1 :int, arg2 :int, arg3 :InvocationService_ResultListener) :void;
}
}
