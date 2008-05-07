//
// $Id$

package com.threerings.msoy.chat.client {

import flash.utils.ByteArray;
import com.threerings.io.TypedArray;
import com.threerings.msoy.chat.client.ChatChannelService;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * An ActionScript version of the Java ChatChannelService interface.
 */
public interface ChatChannelService extends InvocationService
{
    // from Java interface ChatChannelService
    function createChannel (arg1 :Client, arg2 :String, arg3 :InvocationService_ResultListener) :void;

    // from Java interface ChatChannelService
    function inviteToChannel (arg1 :Client, arg2 :MemberName, arg3 :ChatChannel, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface ChatChannelService
    function joinChannel (arg1 :Client, arg2 :ChatChannel, arg3 :InvocationService_ResultListener) :void;

    // from Java interface ChatChannelService
    function leaveChannel (arg1 :Client, arg2 :ChatChannel) :void;
}
}
