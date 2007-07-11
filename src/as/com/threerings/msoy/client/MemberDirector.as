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

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.client.ReportingListener;

public class MemberDirector extends BasicDirector
    implements SetListener
{
    public const log :Log = Log.getLog(MemberDirector);

    public function MemberDirector (ctx :BaseContext)
    {
        super(ctx);
        _bctx = ctx;
    }

    /**
     * Request to make the user our friend, or remove them as a friend.
     */
    public function alterFriend (friendId :int, makeFriend :Boolean) :void
    {
        var listener :ReportingListener = makeFriend ?
            new ReportingListener(_bctx, MsoyCodes.GENERAL_MSGS, null, "m.friend_invited") :
            new ReportingListener(_bctx);

        _msvc.alterFriend(_bctx.getClient(), friendId, makeFriend, listener);
    }

    /**
     * Request to change our display name.
     */
    public function setDisplayName (newName :String) :void
    {
        _msvc.setDisplayName(_bctx.getClient(), newName, new ReportingListener(_bctx));
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var entry :FriendEntry = (event.getEntry() as FriendEntry);
            _bctx.displayInfo(MsoyCodes.GENERAL_MSGS,
                MessageBundle.tcompose("m.friend_added", entry.name));
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        /*
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var entry :FriendEntry = (event.getEntry() as FriendEntry);
            var oldEntry :FriendEntry = (event.getOldEntry() as FriendEntry);
            if (entry.online && !oldEntry.online) {
                _bctx.displayInfo(MsoyCodes.GENERAL_MSGS, MessageBundle.tcompose("m.friend_online", entry.name));
            }
        }
        */
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.FRIENDS) {
            var oldEntry :FriendEntry = (event.getOldEntry() as FriendEntry);
            _bctx.displayInfo(MsoyCodes.GENERAL_MSGS,
                MessageBundle.tcompose("m.friend_removed", oldEntry.name));
        }
    }

    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);

        client.getClientObject().addListener(this);
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.BASE_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        _msvc = (client.requireService(MemberService) as MemberService);
    }

    /**
     * Query the user as to whether they want to make this user their friend.
     */
    protected function approveFriend (asker :MemberName) :void
    {
        // TODO: DHTML-ify
//         new FriendApprovalPanel(_bctx, asker);
    }

    protected var _bctx :BaseContext;
    protected var _msvc :MemberService;
}
}
