//
// $Id$

package com.threerings.msoy.world.client {

import flash.utils.ByteArray;

/**
 * A non-network RoomView for testing avatars and other room entities.
 */
public class RoomStudioView extends RoomView
{
    public function RoomStudioView (ctx :StudioContext, ctrl :RoomStudioController)
    {
        super(ctx, ctrl);

        _sctx = ctx;
    }

    /**
     * This method is needed for anything registered as a "Viewer" in world.mxml.
     */
    public function loadBytes (bytes :ByteArray) :void
    {
        _avatar.setMediaBytes(bytes);
    }

    public function initForViewing (params :Object, uberMode :int) :void
    {
        trace("Got ubermode: " + uberMode);

        (_ctrl as RoomStudioController).studioOnStage();

        var avatar :String = params["avatar"];

        _avatar = new MemberSprite(_ctx, new StudioMemberInfo(_sctx, avatar));
        addSprite(_avatar);
    }

    override public function getMyAvatar () :MemberSprite
    {
        return _avatar;
    }

    public function setAvatarState (state :String) :void
    {
        var studioInfo :StudioMemberInfo = _avatar.getActorInfo().clone() as StudioMemberInfo;
        studioInfo.setState(state);
        _avatar.setOccupantInfo(studioInfo);
    }

    // much TODO

    protected var _sctx :StudioContext;

    protected var _avatar :MemberSprite;
}
}
