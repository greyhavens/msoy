//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;

import mx.containers.TitleWindow;
import mx.controls.Button;
import mx.controls.Text;
import mx.controls.TextArea;
import mx.core.Application;
import mx.managers.PopUpManager;

import com.threerings.util.Controller;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.world.data.MsoySceneModel;

public class HeaderBarController extends Controller
{
    public function HeaderBarController (ctx :WorldContext, headerBar :HeaderBar)
    {
        _ctx = ctx;
        _headerBar = headerBar;
        setControlledPanel(headerBar);
        _ctx.getLocationDirector().addLocationObserver(
            new LocationAdapter(null, this.locationChanged, null));
        _headerBar.addEventListener(Event.ADDED_TO_STAGE, function (evt :Event) :void {
            _ctx.getWorldClient().setWindowTitle(_headerBar.getChatTabs().getLocationName());
        });
    }

    protected function locationChanged (place :PlaceObject) :void
    {
        var scene :Scene = _ctx.getSceneDirector().getScene();
        if (scene != null) {
            _ctx.getWorldClient().setWindowTitle(scene.getName());
            _headerBar.setLocationName(scene.getName());
            // we know the WorldClient is initialized at this point, so it is safe to check whether
            // we are embedded or not.
            _headerBar.setEmbedLinkButtonVisible(!_ctx.getWorldClient().isEmbedded());

            var model :MsoySceneModel = scene.getSceneModel() as MsoySceneModel;
            if (model != null) {
                var svc :MemberService =
                    _ctx.getClient().requireService(MemberService) as MemberService;
                if (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                    svc.getDisplayName(_ctx.getClient(), model.ownerId, new ResultWrapper(
                        function (cause :String) :void {
                            Log.getLog(this).debug("failed to retrieve member owner name: " +
                                cause);
                            _headerBar.setOwnerLink("");
                        },
                        function (res :Object) :void {
                            _headerBar.setOwnerLink(res as String, function () :void {
                                _ctx.getMsoyController().handleViewMember(model.ownerId);
                            });
                        }));
                } else if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                    svc.getGroupName(_ctx.getClient(), model.ownerId, new ResultWrapper(
                        function (cause :String) :void {
                            Log.getLog(this).debug("failed to retrieve group owner name: " +
                                cause);
                            _headerBar.setOwnerLink("");
                        },
                        function (res :Object) :void {
                            _headerBar.setOwnerLink(res as String, function () :void {
                                _ctx.getMsoyController().handleViewGroup(model.ownerId);
                            });
                        }));
                } else {
                    _headerBar.setOwnerLink("");
                }
            }

        } else {
            // For now we can embed scenes with game lobbies attached, but not game instances -
            // when we have a unique URL for game instance locations, then we can embed those
            // locations as well, and have the embedded page bring the game lobby attached to the
            // default game room.
            _headerBar.setEmbedLinkButtonVisible(false);
            var cfg :MsoyGameConfig = _ctx.getGameDirector().getGameConfig();
            if (cfg != null) {
                var name :String = cfg.name;
                _ctx.getWorldClient().setWindowTitle(name);
                _headerBar.setLocationName(name);
                _headerBar.setOwnerLink("");
            }
        }
    }

    protected var _ctx :WorldContext;
    protected var _headerBar :HeaderBar;
}
}
