//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * Handles moving around in the virtual world.
 */
public class WorldDirector extends BasicDirector
{
    public const log :Log = Log.getLog(WorldDirector);

    public function WorldDirector (ctx :WorldContext)
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
     * Request to move to the specified member's current scene.
     *
     * Note: presently the member must be a friend.
     */
    public function goToMemberScene (memberId :int) :void
    {
        _msvc.getCurrentSceneId(_mctx.getClient(), memberId, new ResultWrapper(
            function (cause :String) :void {
                _mctx.displayFeedback(null, cause);
            },
            function (sceneId :int) :void {
                if (sceneId != 0) {
                    _mctx.getSceneDirector().moveTo(sceneId);
                } else {
                }
            }));
    }

    /**
     * Request a change to our avatar.
     *
     * @param newScale a new scale to use, or 0 to retain the avatar's last scale.
     */
    public function setAvatar (avatarId :int, newScale :Number = 0) :void
    {
        _msvc.setAvatar(_mctx.getClient(), avatarId, newScale, new ReportingListener(_mctx));
    }

    /**
     * Request to go to the home of the specified entity.
     */
    protected function goToHome (ownerType :int, ownerId :int) :void
    {
        if (!_mctx.getClient().isLoggedOn()) {
            log.warning("Can't go, not online [type=" + ownerType + ", id=" + ownerId + "].");
            return;
        }
        _msvc.getHomeId(_mctx.getClient(), ownerType, ownerId, new ResultWrapper(
            function (cause :String) :void {
                log.warning("Unable to go to home [type=" + ownerType + ", id=" + ownerId +
                            ", cause=" + cause);
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

    protected var _mctx :WorldContext;
    protected var _msvc :MemberService;
}
}
