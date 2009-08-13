//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java WorldService interface.
 */
public interface WorldService extends InvocationService
{
    // from Java interface WorldService
    function getGroupHomeSceneId (arg1 :int, arg2 :InvocationService_ResultListener) :void;

    // from Java interface WorldService
    function getHomePageGridItems (arg1 :InvocationService_ResultListener) :void;
}
}
