//
// $Id$

package com.threerings.msoy.server;

import com.threerings.msoy.data.MsoyMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link MsoyProvider}.
 */
public class MsoyDispatcher extends InvocationDispatcher<MsoyMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public MsoyDispatcher (MsoyProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public MsoyMarshaller createMarshaller ()
    {
        return new MsoyMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case MsoyMarshaller.DISPATCH_DEFERRED_NOTIFICATIONS:
            ((MsoyProvider)provider).dispatchDeferredNotifications(
                source
            );
            return;

        case MsoyMarshaller.EMAIL_SHARE:
            ((MsoyProvider)provider).emailShare(
                source, ((Boolean)args[0]).booleanValue(), (String)args[1], ((Integer)args[2]).intValue(), (String[])args[3], (String)args[4], (InvocationService.ConfirmListener)args[5]
            );
            return;

        case MsoyMarshaller.GET_ABTEST_GROUP:
            ((MsoyProvider)provider).getABTestGroup(
                source, (String)args[0], ((Boolean)args[1]).booleanValue(), (InvocationService.ResultListener)args[2]
            );
            return;

        case MsoyMarshaller.SET_HEARING_GROUP_CHAT:
            ((MsoyProvider)provider).setHearingGroupChat(
                source, ((Integer)args[0]).intValue(), ((Boolean)args[1]).booleanValue(), (InvocationService.ConfirmListener)args[2]
            );
            return;

        case MsoyMarshaller.TRACK_CLIENT_ACTION:
            ((MsoyProvider)provider).trackClientAction(
                source, (String)args[0], (String)args[1]
            );
            return;

        case MsoyMarshaller.TRACK_TEST_ACTION:
            ((MsoyProvider)provider).trackTestAction(
                source, (String)args[0], (String)args[1]
            );
            return;

        case MsoyMarshaller.TRACK_VECTOR_ASSOCIATION:
            ((MsoyProvider)provider).trackVectorAssociation(
                source, (String)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
