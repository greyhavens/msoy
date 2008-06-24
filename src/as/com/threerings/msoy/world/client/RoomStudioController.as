//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import flash.system.Security;

import com.threerings.io.TypedArray;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.RoomConfig;

public class RoomStudioController extends RoomController
{
    /**
     * Called by the view once it's on stage.
     */
    public function studioOnStage () :void
    {
        _walkTarget.visible = false;
        _flyTarget.visible = false;
        _roomView.addChildAt(_flyTarget, _roomView.numChildren);
        _roomView.addChildAt(_walkTarget, _roomView.numChildren);
    
        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(Event.ENTER_FRAME, checkMouse, false, int.MIN_VALUE);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);

        setControlledPanel(_studioView);
    }

    // documentation inherited
    override public function handleAvatarClicked (avatar :MemberSprite) :void
    {
        var menuItems :Array = [];
        addSelfMenuItems(avatar, menuItems, true);
        if (menuItems.length == 0) {
            menuItems.push({ label: Msgs.GENERAL.get("l.noAvActions"), enabled: false });
        }
        popActorMenu(avatar, menuItems);
    }

    // documentation inherited
    override public function doAvatarAction (action :String) :void
    {
        _studioView.getMyAvatar().messageReceived(action, null, true);
    }

    // documentation inherited
    override public function doAvatarState (state :String) :void
    {
        _studioView.setAvatarState(state);
    }

    // documentation inherited
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _studioView = new RoomStudioView(ctx as StudioContext, this);
        _roomView = _studioView; // also copy it to that

        // and.. initialize it!
        var model :MsoySceneModel = new MsoySceneModel();
        model.ownerType = MsoySceneModel.OWNER_TYPE_MEMBER;
        model.furnis = TypedArray.create(FurniData);
        model.decor = MsoySceneModel.defaultMsoySceneModelDecor();

        // TODO: sort out what exactly we want to do when we're run from disk
        if (Security.sandboxType == Security.LOCAL_WITH_FILE) {
            model.decor.furniMedia = null; // this will cause a black room to be shown
        }
        _scene = new MsoyScene(model, _config);
        _studioView.setScene(_scene);
        _studioView.setBackground(model.decor);

        return _studioView;
    }

    override protected function requestAvatarMove (newLoc :MsoyLocation) :void
    {
        _studioView.getMyAvatar().moveTo(newLoc, _scene);
    }

    protected var _studioView :RoomStudioView;
}
}
