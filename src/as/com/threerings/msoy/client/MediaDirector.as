//
// $Id$

package com.threerings.msoy.client {

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.ClientEvent;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.world.client.AvatarSprite;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.MsoySprite;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.WorldMemberInfo;

public class MediaDirector extends BasicDirector
{
    public function MediaDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
    }

    /**
     * Get an avatar sprite for the specified occupant info, caching as appropriate.
     */
    public function getAvatar (occInfo :WorldMemberInfo) :AvatarSprite
    {
        var isOurs :Boolean = (occInfo.bodyOid == _ctx.getClient().getClientOid());
        if (isOurs && _ourAvatar != null) {
            _ourAvatar.setItemIdent(_mctx.getClientObject().avatar.getIdent());
            _ourAvatar.setOccupantInfo(_mctx, occInfo);
            return _ourAvatar;
        }

        var avatar :AvatarSprite = new AvatarSprite(_mctx, occInfo);
        if (isOurs) {
            _ourAvatar = avatar;
            _ourAvatar.setItemIdent(_mctx.getClientObject().avatar.getIdent());
        }
        return avatar;
    }

    /**
     * Get a Furni sprite for the specified furni data, caching as appropriate.
     */
    public function getFurni (furni :FurniData) :FurniSprite
    {
        return new FurniSprite(_mctx, furni);
    }

    /**
     * Release any references to the specified sprite, if appropriate.
     */
    public function returnSprite (sprite :MsoySprite) :void
    {
        if (sprite != _ourAvatar) {
            sprite.shutdown();

        } else {
            // prevent it from continuing to move, but don't shut it down
            _ourAvatar.stopMove();
        }
    }

    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        // release our hold on our avatar
        _ourAvatar = null;
    }

    /** A casted copy of the context. */
    protected var _mctx :MsoyContext;

    /** Our very own avatar: avoid loading and unloading it. */
    protected var _ourAvatar :AvatarSprite;
}
}
