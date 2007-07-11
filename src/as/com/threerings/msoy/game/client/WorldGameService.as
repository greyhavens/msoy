//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;
import com.threerings.io.TypedArray;
import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java WorldGameService interface.
 */
public interface WorldGameService extends InvocationService
{
    // from Java interface WorldGameService
    function joinWorldGame (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void;

    // from Java interface WorldGameService
    function leaveWorldGame (arg1 :Client, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface WorldGameService
    function updateMemory (arg1 :Client, arg2 :MemoryEntry) :void;
}
}
