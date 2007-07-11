//
// $Id$

package com.threerings.msoy.server;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.data.MemberMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link MemberProvider}.
 */
public class MemberDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public MemberDispatcher (MemberProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new MemberMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case MemberMarshaller.ACKNOWLEDGE_NOTIFICATIONS:
            ((MemberProvider)provider).acknowledgeNotifications(
                source,
                (int[])args[0], (InvocationService.InvocationListener)args[1]
            );
            return;

        case MemberMarshaller.ALTER_FRIEND:
            ((MemberProvider)provider).alterFriend(
                source,
                ((Integer)args[0]).intValue(), ((Boolean)args[1]).booleanValue(), (InvocationService.ConfirmListener)args[2]
            );
            return;

        case MemberMarshaller.GET_CURRENT_SCENE_ID:
            ((MemberProvider)provider).getCurrentSceneId(
                source,
                ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case MemberMarshaller.GET_DISPLAY_NAME:
            ((MemberProvider)provider).getDisplayName(
                source,
                ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case MemberMarshaller.GET_GROUP_NAME:
            ((MemberProvider)provider).getGroupName(
                source,
                ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case MemberMarshaller.GET_HOME_ID:
            ((MemberProvider)provider).getHomeId(
                source,
                ((Byte)args[0]).byteValue(), ((Integer)args[1]).intValue(), (InvocationService.ResultListener)args[2]
            );
            return;

        case MemberMarshaller.SET_AVATAR:
            ((MemberProvider)provider).setAvatar(
                source,
                ((Integer)args[0]).intValue(), ((Float)args[1]).floatValue(), (InvocationService.InvocationListener)args[2]
            );
            return;

        case MemberMarshaller.SET_DISPLAY_NAME:
            ((MemberProvider)provider).setDisplayName(
                source,
                (String)args[0], (InvocationService.InvocationListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
