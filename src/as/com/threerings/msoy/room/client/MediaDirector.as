//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.util.Log;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.ClientEvent;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.avrg.client.AVRGameBackend;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MobInfo;
import com.threerings.msoy.room.data.ObserverInfo;
import com.threerings.msoy.room.data.PetInfo;
import com.threerings.msoy.room.data.RoomObject;

/**
 * Handles the loading of various media.
 */
public class MediaDirector extends BasicDirector
{
    public static const log :Log = Log.getLog(MediaDirector);

    public function MediaDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;

        ctx.getLocationDirector().addLocationObserver(new LocationAdapter(null, locationDidChange));
    }

    /**
     * Creates an occupant sprite for the specified occupant info.
     */
    public function getSprite (occInfo :OccupantInfo) :OccupantSprite
    {
        if (occInfo is MemberInfo) {
            var isOurs :Boolean = _wctx.getMyName().equals(occInfo.username);
            if (isOurs && _ourAvatar != null) {
                _ourAvatar.setOccupantInfo(occInfo);
                return _ourAvatar;
            }
            var sprite :MemberSprite = new MemberSprite(_wctx, occInfo as MemberInfo);
            if (isOurs) {
                _ourAvatar = sprite;
            }
            return sprite;

        } else if (occInfo is PetInfo) {
            return new PetSprite(_wctx, occInfo as PetInfo);

        } else if (occInfo is MobInfo) {
            if (MobInfo(occInfo).getGameId() == _wctx.getGameDirector().getGameId()) {
                return new MobSprite(_wctx, occInfo as MobInfo,
                    _wctx.getGameDirector().getAVRGameBackend());
            }
            return null;

        } else if (occInfo is ObserverInfo) {
            // view-only members have no sprite visualization
            return null;

        // NOTE: if you add a new type here, please also add it to src/thane/Reference.as so that 
        // server side code for avrg's doesn't break

        } else {
            log.warning("Don't know how to create sprite for occupant " + occInfo + ".");
            return null;
        }
    }

    /**
     * Get a Furni sprite for the specified furni data, caching as appropriate.
     */
    public function getFurni (furni :FurniData) :FurniSprite
    {
        return new FurniSprite(_wctx, furni);
    }

    /**
     * Get a Decor sprite for the specified decor data, caching as appropriate.
     */
    public function getDecor (decor :Decor) :DecorSprite
    {
        return new DecorSprite(_wctx, decor);
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

        shutdownOurAvatar();
    }

    /**
     * This method is adapted as a LocationObserver method.
     */
    protected function locationDidChange (place :PlaceObject) :void
    {
        // if we've moved to a non-room, kill our avatar
        if (!(place is RoomObject)) {
            shutdownOurAvatar();
        }
    }

    protected function shutdownOurAvatar () :void
    {
        // release our hold on our avatar
        if (_ourAvatar != null) {
            _ourAvatar.shutdown();
            _ourAvatar = null;
        }
    }

    /** A casted copy of the context. */
    protected var _wctx :WorldContext;

    /** Our very own avatar: avoid loading and unloading it. */
    protected var _ourAvatar :MemberSprite;
}
}
