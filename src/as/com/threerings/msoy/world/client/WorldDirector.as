//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * Handles moving around in the virtual world.
 */
public class WorldDirector extends BasicDirector
{
    public const log :Log = Log.getLog(WorldDirector);

    public function WorldDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
    }

    /**
     * Request to move to the specified member's home.
     */
    public function goToMemberHome (memberId :int) :void
    {
        goToHome(MsoySceneModel.OWNER_TYPE_MEMBER, memberId);
    }

    /**
     * Request to move to the specified group's home.
     */
    public function goToGroupHome (groupId :int) :void
    {
        goToHome(MsoySceneModel.OWNER_TYPE_GROUP, groupId);
    }

    /**
     * Request a change to our avatar.
     */
    public function setAvatar (avatarId :int) :void
    {
        _msvc.setAvatar(_mctx.getClient(), avatarId, new ReportingListener(_mctx));
    }

    /**
     * Request to purchase a new room.
     */
    public function purchaseRoom () :void
    {
        _msvc.purchaseRoom(
            _mctx.getClient(), new ReportingListener(_mctx, null, null, "m.room_created"));
    }

    /**
     * Request to go to the home of the specified entity.
     */
    protected function goToHome (ownerType :int, ownerId :int) :void
    {
        _msvc.getHomeId(_mctx.getClient(), ownerType, ownerId, new ResultWrapper(
            function (cause :String) :void {
                log.warning("Unable to go to home: " + cause);
            }, 
            function (sceneId :int) :void {
                _mctx.getSceneDirector().moveTo(sceneId);
            }));
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.WORLD_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        // TODO: move the functions we use into a WorldService
        _msvc = (client.requireService(MemberService) as MemberService);
    }

    protected var _mctx :MsoyContext;
    protected var _msvc :MemberService;
}
}
