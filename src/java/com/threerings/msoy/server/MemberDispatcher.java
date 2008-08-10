//
// $Id$

package com.threerings.msoy.server;

import com.threerings.msoy.data.MemberMarshaller;
import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link MemberProvider}.
 */
public class MemberDispatcher extends InvocationDispatcher<MemberMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public MemberDispatcher (MemberProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public MemberMarshaller createMarshaller ()
    {
        return new MemberMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case MemberMarshaller.ACKNOWLEDGE_WARNING:
            ((MemberProvider)provider).acknowledgeWarning(
                source
            );
            return;

        case MemberMarshaller.BOOT_FROM_PLACE:
            ((MemberProvider)provider).bootFromPlace(
                source, ((Integer)args[0]).intValue(), (InvocationService.ConfirmListener)args[1]
            );
            return;

        case MemberMarshaller.COMPLAIN_MEMBER:
            ((MemberProvider)provider).complainMember(
                source, ((Integer)args[0]).intValue(), (String)args[1]
            );
            return;

        case MemberMarshaller.EMAIL_SHARE:
            ((MemberProvider)provider).emailShare(
                source, ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), (String[])args[2], (String)args[3], (InvocationService.ConfirmListener)args[4]
            );
            return;

        case MemberMarshaller.FOLLOW_MEMBER:
            ((MemberProvider)provider).followMember(
                source, ((Integer)args[0]).intValue(), (InvocationService.ConfirmListener)args[1]
            );
            return;

        case MemberMarshaller.GET_ABTEST_GROUP:
            ((MemberProvider)provider).getABTestGroup(
                source, (ReferralInfo)args[0], (String)args[1], ((Boolean)args[2]).booleanValue(), (InvocationService.ResultListener)args[3]
            );
            return;

        case MemberMarshaller.GET_CURRENT_MEMBER_LOCATION:
            ((MemberProvider)provider).getCurrentMemberLocation(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case MemberMarshaller.GET_DISPLAY_NAME:
            ((MemberProvider)provider).getDisplayName(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case MemberMarshaller.GET_GROUP_HOME_SCENE_ID:
            ((MemberProvider)provider).getGroupHomeSceneId(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case MemberMarshaller.GET_GROUP_NAME:
            ((MemberProvider)provider).getGroupName(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case MemberMarshaller.GET_HOME_ID:
            ((MemberProvider)provider).getHomeId(
                source, ((Byte)args[0]).byteValue(), ((Integer)args[1]).intValue(), (InvocationService.ResultListener)args[2]
            );
            return;

        case MemberMarshaller.INVITE_TO_BE_FRIEND:
            ((MemberProvider)provider).inviteToBeFriend(
                source, ((Integer)args[0]).intValue(), (InvocationService.ConfirmListener)args[1]
            );
            return;

        case MemberMarshaller.INVITE_TO_FOLLOW:
            ((MemberProvider)provider).inviteToFollow(
                source, ((Integer)args[0]).intValue(), (InvocationService.ConfirmListener)args[1]
            );
            return;

        case MemberMarshaller.SET_AVATAR:
            ((MemberProvider)provider).setAvatar(
                source, ((Integer)args[0]).intValue(), ((Float)args[1]).floatValue(), (InvocationService.ConfirmListener)args[2]
            );
            return;

        case MemberMarshaller.SET_AWAY:
            ((MemberProvider)provider).setAway(
                source, ((Boolean)args[0]).booleanValue(), (String)args[1]
            );
            return;

        case MemberMarshaller.SET_DISPLAY_NAME:
            ((MemberProvider)provider).setDisplayName(
                source, (String)args[0], (InvocationService.InvocationListener)args[1]
            );
            return;

        case MemberMarshaller.SET_HOME_SCENE_ID:
            ((MemberProvider)provider).setHomeSceneId(
                source, ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), ((Integer)args[2]).intValue(), (InvocationService.ConfirmListener)args[3]
            );
            return;

        case MemberMarshaller.TRACK_CLIENT_ACTION:
            ((MemberProvider)provider).trackClientAction(
                source, (ReferralInfo)args[0], (String)args[1], (String)args[2]
            );
            return;

        case MemberMarshaller.TRACK_REFERRAL_CREATION:
            ((MemberProvider)provider).trackReferralCreation(
                source, (ReferralInfo)args[0]
            );
            return;

        case MemberMarshaller.TRACK_TEST_ACTION:
            ((MemberProvider)provider).trackTestAction(
                source, (ReferralInfo)args[0], (String)args[1], (String)args[2]
            );
            return;

        case MemberMarshaller.UPDATE_AVAILABILITY:
            ((MemberProvider)provider).updateAvailability(
                source, ((Integer)args[0]).intValue()
            );
            return;

        case MemberMarshaller.UPDATE_STATUS:
            ((MemberProvider)provider).updateStatus(
                source, (String)args[0], (InvocationService.InvocationListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
