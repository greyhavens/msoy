//
// $Id$

package com.threerings.msoy.chat.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;

import com.threerings.util.Name;

import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.chat.data.ChatChannel;
import com.threerings.crowd.chat.server.ChatChannelManager;
import com.threerings.crowd.data.BodyObject;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberLocator;

import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.chat.data.MsoyChatChannel;

/**
 * Customizes the Crowd chat channel services for Whirled.
 */
@Singleton
public class MsoyChatChannelManager extends ChatChannelManager
{
    @Inject public MsoyChatChannelManager (InvocationManager invmgr)
    {
        super(invmgr);
    }

    @Override // from ChatChannelManager
    protected int getBodyId (Name speaker)
    {
        return ((MemberName)speaker).getMemberId();
    }

    @Override // from ChatChannelManager
    protected BodyObject getBodyObject (int bodyId)
    {
        return _locator.lookupMember(bodyId);
    }

    @Override // from ChatChannelManager
    protected void finishResolveAndDispatch (ChatChannel channel)
    {
        if (!(channel instanceof MsoyChatChannel)) {
            resolutionFailed(channel, new Exception("Unknown channel type " + channel.getClass()));
            return;
        }

        MsoyChatChannel mchannel = (MsoyChatChannel)channel;
        switch (mchannel.type) {
        case MsoyChatChannel.GROUP_CHANNEL:
            finishResolveGroupChannel(mchannel);
            break;
        case MsoyChatChannel.PRIVATE_CHANNEL:
            finishResolvePrivateChannel(mchannel);
            break;
        default:
            resolutionFailed(channel, new Exception("Invalid channel type"));
            break;
        }
    }

    protected void finishResolveGroupChannel (final MsoyChatChannel channel)
    {
        _invoker.postUnit(new RepositoryUnit("resolveGroup(" + channel + ")") {
            public void invokePersist () throws Exception {
                int groupId = ((GroupName)channel.ident).getGroupId();
                for (GroupMembershipRecord gmr : _groupRepo.getMembers(groupId)) {
                    _memberIds.add(gmr.memberId);
                }
            }
            public void handleSuccess () {
                resolutionComplete(channel, _memberIds);
            }
            public void handleFailure (Exception e) {
                resolutionFailed(channel, e);
            }
            protected ArrayIntSet _memberIds = new ArrayIntSet();
        });
    }

    protected void finishResolvePrivateChannel (final MsoyChatChannel channel)
    {
        resolutionFailed(channel, new Exception("Private channels not yet implemented"));
    }

    @Inject protected MemberLocator _locator;
    @Inject protected GroupRepository _groupRepo;
}
