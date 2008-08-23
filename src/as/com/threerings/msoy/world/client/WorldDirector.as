//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.Log;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.crowd.client.LocationAdapter;

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.PetMarshaller;
import com.threerings.msoy.room.data.RoomConfig;

/**
 * Handles moving around in the virtual world.
 */
public class WorldDirector extends BasicDirector
{
    public const log :Log = Log.getLog(WorldDirector);

    public function WorldDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;

        // ensure that the compiler includes these necessary symbols
        var c :Class;
        c = PetMarshaller;
        c = RoomConfig;
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
    public function goToMemberLocation (memberId :int, location :MemberLocation = null) :void
    {
        if (location != null) {
            finishGoToMemberLocation(location);
            return;
        } 

        _msvc.getCurrentMemberLocation(_wctx.getClient(), memberId, new ResultWrapper(
            function (cause :String) :void {
                _wctx.displayFeedback(null, cause);
            },
            finishGoToMemberLocation));
    }

    /**
     * Request a change to our avatar.
     *
     * @param newScale a new scale to use, or 0 to retain the avatar's last scale.
     */
    public function setAvatar (avatarId :int, newScale :Number = 0) :void
    {
        _msvc.setAvatar(_wctx.getClient(), avatarId, newScale, new ConfirmAdapter(
            function (cause :String) :void {
                log.info("Reporting failure [reason=" + cause + "].");
                _wctx.displayFeedback(null, cause);
            },
            function () :void {
                _wctx.getGameDirector().tutorialEvent("avatarInstalled");
            }));
    }

    /**
     * Request to go to the home of the specified entity.
     */
    protected function goToHome (ownerType :int, ownerId :int) :void
    {
        if (!_wctx.getClient().isLoggedOn()) {
            log.info("Delaying goToHome, not online [type=" + ownerType + ", id=" + ownerId + "].");
            var waiter :ClientAdapter = new ClientAdapter(null, function (event :*) :void {
                _wctx.getClient().removeClientObserver(waiter);
                goToHome(ownerType, ownerId);
            });
            _wctx.getClient().addClientObserver(waiter);
            return;
        }
        _msvc.getHomeId(_wctx.getClient(), ownerType, ownerId, new ResultWrapper(
            function (cause :String) :void {
                log.warning("Unable to go to home [type=" + ownerType + ", id=" + ownerId +
                            ", cause=" + cause);
            }, 
            function (sceneId :int) :void {
                _wctx.getSceneDirector().moveTo(sceneId);
            }));
    }

    /**
     * Called by {@link #goToMemberLocation}.
     */
    protected function finishGoToMemberLocation (location :MemberLocation) :void
    {
        var goToGame :Function = function () :void {};
        // TODO: Do something more interesting for AVR Games.
        if (!location.avrGame && location.gameId != 0) {
            goToGame = function () :void {
                _wctx.getGameDirector().joinPlayer(location.gameId, location.memberId);
            };
        }

        var sceneId :int = location.sceneId;
        if (sceneId == 0 && _wctx.getSceneDirector().getScene() == null) {
            // if we're not in a scene and they're not in a scene, go home.  If they're in an
            // unwatchable game, we'll get an error in the lobby, and this way we'll at least be in
            // a scene as well
            sceneId = _wctx.getMemberObject().getHomeSceneId();
        }

        if (sceneId == 0) {
            goToGame(); // we're not moving, so take our game action immediately
            return;
        }

        // otherwise we have to do things the hard way
        var gameLauncher :LocationAdapter;
        gameLauncher = new LocationAdapter(null, function (...ignored) :void {
            _wctx.getLocationDirector().removeLocationObserver(gameLauncher);
            goToGame();
        }, null);
        _wctx.getLocationDirector().addLocationObserver(gameLauncher);
        _wctx.getWorldController().handleGoScene(location.sceneId);
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

    protected var _wctx :WorldContext;
    protected var _msvc :MemberService;
}
}
