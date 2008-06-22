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

        _avatar = new MemberSprite(_ctx, new StudioInfo(_sctx, avatar));
        addSprite(_avatar);
    }

    override public function getMyAvatar () :MemberSprite
    {
        return _avatar;
    }

    // much TODO

    protected var _sctx :StudioContext;

    protected var _avatar :MemberSprite;
}
}

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.world.client.StudioContext;
import com.threerings.msoy.world.data.MemberInfo;

class StudioInfo extends MemberInfo
{
    public function StudioInfo (ctx :StudioContext, avatarUrl :String)
    {
        username = ctx.getMyName();
        _media = new StudioMediaDesc(avatarUrl);
        _scale = 1;
    }
}

class StudioMediaDesc extends MediaDesc
{
    public function StudioMediaDesc (avatarUrl :String)
    {
        _url = avatarUrl;
    }

    override public function getMediaId () :String
    {
        return "studio";
    }

    override public function getMediaPath () :String
    {
        return _url;
    }

    protected var _url :String;
}
