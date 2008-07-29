//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.msoy.badge.data.EarnedBadge;
import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.data.MemberMarshaller;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.util.Log;

public class MemberDirector extends BasicDirector
{
    public const log :Log = Log.getLog(MemberDirector);

    public function MemberDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;

        // ensure that the compiler includes these necessary symbols
        var c :Class = MemberMarshaller;
    }

    /**
     * Request to make the user our friend.
     */
    public function inviteToBeFriend (friendId :int) :void
    {
        _msvc.inviteToBeFriend(_wctx.getClient(), friendId, new ConfirmAdapter(
            function (cause :String) :void {
                log.info("Reporting failure [reason=" + cause + "].");
                _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS, cause);
             },
             function () :void {
                 _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS, "m.friend_invited");
                 _wctx.getGameDirector().tutorialEvent("friendInvited");
             }));
    }

    /**
     * Request to change our display name.
     */
    public function setDisplayName (newName :String) :void
    {
        _msvc.setDisplayName(_wctx.getClient(), newName, new ReportingListener(_wctx));
    }

    // from BasicDirector
    override protected function clientObjectUpdated (client :Client) :void
    {
        if (_mobj != null) {
            _mobj.removeListener(_memberListener);
        }
        _mobj = client.getClientObject() as MemberObject;
        if (_mobj != null) {
            _mobj.addListener(_memberListener);
        }
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.MEMBER_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        _msvc = (client.requireService(MemberService) as MemberService);
    }

    protected function messageReceivedOnUserObject (event :MessageEvent) :void
    {
        if (event.getName() == MemberObject.BADGE_AWARDED) {
            var badge :EarnedBadge = event.getArgs()[0] as EarnedBadge;
            // TODO - show a clever display here
            log.info("Badge awarded", badge);
        }
    }

    protected var _wctx :WorldContext;
    protected var _msvc :MemberService;
    protected var _mobj :MemberObject;
    protected var _memberListener :MessageAdapter = new MessageAdapter(messageReceivedOnUserObject);
}
}
