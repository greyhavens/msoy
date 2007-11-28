//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;
import com.threerings.io.TypedArray;
import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.client.MsoyGameService_LocationListener;
import com.threerings.msoy.game.data.MsoyGameMarshaller_LocationMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java MsoyGameService interface.
 */
public interface MsoyGameService extends InvocationService
{
    // from Java interface MsoyGameService
    function inviteFriends (arg1 :Client, arg2 :int, arg3 :TypedArray /* of int */) :void;

    // from Java interface MsoyGameService
    function locateGame (arg1 :Client, arg2 :int, arg3 :MsoyGameService_LocationListener) :void;
}
}
