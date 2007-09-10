//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;
import com.threerings.io.TypedArray;
import com.threerings.msoy.game.client.AVRGameService;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java AVRGameService interface.
 */
public interface AVRGameService extends InvocationService
{
    // from Java interface AVRGameService
    function joinAVRGame (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void;

    // from Java interface AVRGameService
    function leaveAVRGame (arg1 :Client, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface AVRGameService
    function updateMemory (arg1 :Client, arg2 :MemoryEntry) :void;
}
}
