//
// $Id$

package com.threerings.msoy.client {

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MemberName;

import com.threerings.msoy.chat.client.ReportingListener;

public class MemberDirector extends BasicDirector
    implements SetListener
{
    public const log :Log = Log.getLog(MemberDirector);

    public function MemberDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
    }

    /**
     * Request to move to the specified member's home.
     */
    public function goToMemberHome (memberId :int) :void
    {
        _msvc.getMemberHomeId(_mctx.getClient(), memberId, new ResultWrapper(
            function (cause :String) :void {
                log.warning("Unable to go to friend's home: " + cause);
            }, 
            function (sceneId :int) :void {
                _mctx.getSceneDirector().moveTo(sceneId);
            }));
    }

    /**
     * Request to make the user our friend, or remove them as a friend.
     */
    public function alterFriend (friendId :int, makeFriend :Boolean) :void
    {
        _msvc.alterFriend(_mctx.getClient(), friendId, makeFriend,
            new ReportingListener(_mctx));
    }

    /**
     * Request to change our display name.
     */
    public function setDisplayName (newName :String) :void
    {
        _msvc.setDisplayName(_mctx.getClient(), newName,
            new ReportingListener(_mctx));
    }

    /**
     * Request a change to our avatar.
     */
    public function setAvatar (avatarId :int) :void
    {
        _msvc.setAvatar(_mctx.getClient(), avatarId,
            new ReportingListener(_mctx));
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var entry :FriendEntry = (event.getEntry() as FriendEntry);
            switch (entry.status) {
            case FriendEntry.PENDING_MY_APPROVAL:
                approveFriend(entry.name);
                break;
            }
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var entry :FriendEntry = (event.getEntry() as FriendEntry);
            var oldEntry :FriendEntry = (event.getOldEntry() as FriendEntry);
            var msgKey :String = null;
            if (entry.status == FriendEntry.FRIEND) {
                if (oldEntry.status != FriendEntry.FRIEND) {
                    msgKey = "m.friend_confirmed";

                } else if (entry.online && !oldEntry.online) {
                    msgKey = "m.friend_online";
                }
            }
            if (msgKey != null) {
                _mctx.displayInfo("general",
                    MessageBundle.tcompose(msgKey, entry.name));
            }
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var oldEntry :FriendEntry = (event.getOldEntry() as FriendEntry);
            var msgKey :String;
            switch (oldEntry.status) {
            case FriendEntry.FRIEND:
                msgKey = "m.friend_removed";
                break;

            case FriendEntry.PENDING_THEIR_APPROVAL:
                // TODO: we don't want this to be reported when
                // WE initiate the removal
                msgKey = "m.friend_denied";
                break;

            default:
                return; // do nothing in every other case
            }
            _mctx.displayInfo("general",
                MessageBundle.tcompose(msgKey, oldEntry.name));
        }
    }

    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);

        client.getClientObject().addListener(this);
    }

    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        _msvc = (client.requireService(MemberService) as MemberService);
    }

    /**
     * Query the user as to whether they want to make this user their
     * friend.
     */
    protected function approveFriend (asker :MemberName) :void
    {
        new FriendApprovalPanel(_mctx, asker);
    }

    protected var _mctx :MsoyContext;

    protected var _msvc :MemberService;
}
}
