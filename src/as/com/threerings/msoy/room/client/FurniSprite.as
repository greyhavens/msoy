//
// $Id$

package com.threerings.msoy.room.client {
import flash.display.LoaderInfo;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.util.CommandEvent;
import com.threerings.util.ValueEvent;

import com.threerings.media.MediaContainer;

import com.threerings.msoy.client.LoadingWatcher;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.world.client.WorldContext;

public class FurniSprite extends EntitySprite
{
    /**
     * Set the LoadingWatcher that will be used to track whether any
     * FurniSprites are currently loading.
     */
    public static function setLoadingWatcher (watcher :LoadingWatcher) :void
    {
        _loadingWatcher = watcher;
    }

    /**
     * Construct a new FurniSprite.
     */
    public function FurniSprite (ctx :WorldContext, furni :FurniData)
    {
        super(ctx);
        _furni = furni;

        // configure our media and item
        setItemIdent(furni.getItemIdent());
        _sprite.setSpriteMediaScale(furni.scaleX, furni.scaleY);
        _sprite.setMediaDesc(furni.media);

        // set up our hotspot if one is configured in the furni data record
        if (_furni.hotSpotX > 0 || _furni.hotSpotY > 0) {
            _hotSpot = new Point(_furni.hotSpotX, _furni.hotSpotY);
        }

        _sprite.addEventListener(MouseEvent.ROLL_OVER, handleMouseHover);
        _sprite.addEventListener(MouseEvent.ROLL_OUT, handleMouseHover);
        _sprite.addEventListener(MediaContainer.LOADER_READY, handleLoaderReady);
    }

    override public function getDesc () :String
    {
        switch (_furni.actionType) {
        case FurniData.ACTION_PORTAL:
            return "m.portal";

        case FurniData.ACTION_LOBBY_GAME:
        case FurniData.ACTION_WORLD_GAME:
            return "m.game";

        default:
            return "m.furni";
        }
    }

    public function getFurniData () :FurniData
    {
        return _furni;
    }

    /** Can this sprite be removed from the room? */
    public function isRemovable () :Boolean
    {
        return true;
    }

    /** Can this sprite's action be modified? */
    public function isActionModifiable () :Boolean
    {
        // game furnis can't be turned into something else - but everything else can
        return ! (_furni.actionType == FurniData.ACTION_LOBBY_GAME ||
                  _furni.actionType == FurniData.ACTION_WORLD_GAME);
    }

    public function update (furni :FurniData) :void
    {
        _furni = furni;
        setItemIdent(furni.getItemIdent());
        _sprite.setMediaDesc(furni.media);
        scaleUpdated();
        rotationUpdated();
        setLocation(furni.loc);
    }

    override public function getToolTipText () :String
    {
        // clear out any residuals from the last action
        var actionData :Array = _furni.splitActionData();

        switch (_furni.actionType) {
        case FurniData.ACTION_NONE:
            // no tooltip
            return null;

        case FurniData.ACTION_URL:
            // if there's no description, use the URL
            return String(actionData[actionData.length - 1]);

        case FurniData.ACTION_LOBBY_GAME:
        case FurniData.ACTION_WORLD_GAME:
            return Msgs.GENERAL.get("i.play_game", String(actionData[1]));

        case FurniData.ACTION_PORTAL:
            return Msgs.GENERAL.get("i.trav_portal", String(actionData[actionData.length-1]));

        case FurniData.ACTION_HELP_PAGE:
            return Msgs.GENERAL.get("i.help_page", String(actionData[0]));

        default:
            log.warning("Tooltip: unknown furni action type",
                "actionType", _furni.actionType, "actionData", _furni.actionData);
            return null;
        }
    }

    /**
     * If we're a portal furniture, called to animate a player entering
     * or leaving.
     */
    public function wasTraversed (entering :Boolean) :void
    {
        // TODO: pass body as arg? Pass dimensions of body as arg?
        // TODO: receive a path or some other descriptor of an animation
        //       for the body???

        // Note: these constants are defined in FurniControl, but there's  no way to reference
        // that without that class being compiled in, and constants are not inlined.
        // So- we've made the decision to a) Duplicate and b) Don't fuck up step a.
        messageReceived(entering ? "bodyEntered" : "bodyLeft", null, true);
    }

    override protected function scaleUpdated () :void
    {
        // update our visualization with the furni's current scale
        _sprite.setSpriteMediaScale(_furni.scaleX, _furni.scaleY);

        super.scaleUpdated();
    }

    override protected function createBackend () :EntityBackend
    {
        return new FurniBackend();
    }

    override protected function useLocationScale () :Boolean
    {
        return !_furni.isNoScale();
    }

    override public function getMediaRotation () :Number
    {
        return _furni.rotation;
    }

    override public function setMediaRotation (rotation :Number) :void
    {
        _furni.rotation = rotation;
        rotationUpdated();
    }

    override public function setMediaScaleX (scaleX :Number) :void
    {
        _furni.scaleX = scaleX;
        scaleUpdated();
    }

    override public function setMediaScaleY (scaleY :Number) :void
    {
        _furni.scaleY = scaleY;
        scaleUpdated();
    }

    // documentation inherited
    override public function hasAction () :Boolean
    {
        switch (_furni.actionType) {
        case FurniData.ACTION_NONE:
            return false;

        default:
            return true;
        }
    }

    override public function capturesMouse () :Boolean
    {
        switch (_furni.actionType) {
        case FurniData.ACTION_NONE:
            return (_furni.actionData == null);

        default:
            return super.capturesMouse();
        }
    }

    override public function toString () :String
    {
        return "FurniSprite[" + _furni.itemType + ":" + _furni.itemId + "]";
    }

    // documentation inherited
    override public function getHoverColor () :uint
    {
        switch (_furni.actionType) {
        case FurniData.ACTION_PORTAL:
            return PORTAL_HOVER;

        case FurniData.ACTION_LOBBY_GAME:
        case FurniData.ACTION_WORLD_GAME:
            return GAME_HOVER;

        default:
            return OTHER_HOVER;
        }
    }

    override protected function postClickAction () :void
    {
        if (hasAction()) {
            CommandEvent.dispatch(_sprite, RoomController.FURNI_CLICKED, _furni);
        }
    }

    /**
     * Listens for ROLL_OVER and ROLL_OUT, which we only receive if the sprite
     * has action.
     */
    protected function handleMouseHover (event :MouseEvent) :void
    {
        callUserCode("mouseHover_v1", (event.type == MouseEvent.ROLL_OVER));
    }

    protected function handleLoaderReady (event :ValueEvent) :void
    {
        var info :LoaderInfo = (event.value as LoaderInfo);

        if (_loadingWatcher != null) {
            log.info("Setting up ")
            _loadingWatcher.watchLoader(info, _sprite, (_sprite is DecorSprite));
        }
    }

    /** The furniture data for this piece of furni. */
    protected var _furni :FurniData;

    /** The watcher for loading progress. */
    protected static var _loadingWatcher :LoadingWatcher;
}
}
