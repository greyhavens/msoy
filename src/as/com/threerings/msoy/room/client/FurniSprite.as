//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;
import flash.display.LoaderInfo;

import flash.events.MouseEvent;

import flash.geom.Point;

import com.threerings.util.CommandEvent;

import com.threerings.msoy.client.LoadingWatcher;
import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.FurniData;

public class FurniSprite extends MsoySprite
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
        setMediaDesc(furni.media);

        // set up our hotspot if one is configured in the furni data record
        if (_furni.hotSpotX > 0 || _furni.hotSpotY > 0) {
            _hotSpot = new Point(_furni.hotSpotX, _furni.hotSpotY);
        }

        checkPerspective();

        addEventListener(MouseEvent.ROLL_OVER, handleMouseHover);
        addEventListener(MouseEvent.ROLL_OUT, handleMouseHover);
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

    public function isPerspectable () :Boolean
    {
        return (_desc != null) && _desc.isImage();
    }

    public function isPerspectivized () :Boolean
    {
        return _furni.isPerspective();
    }

    public function togglePerspective () :void
    {
        _furni.setPerspective(!isPerspectivized());
        checkPerspective();
        scaleUpdated();
    }

    public function update (furni :FurniData) :void
    {
        _furni = furni;
        setItemIdent(furni.getItemIdent());
        setMediaDesc(furni.media);
        checkPerspective();
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
            log.warning("Tooltip requested for unhandled furni action type " +
                "[actionType=" + _furni.actionType +
                ", actionData=" + _furni.actionData + "].");
            return null;
        }
    }

    /**
     * Get a custom configuration panel for this piece of furniture, if any.
     */
    public function getCustomConfigPanel () :DisplayObject
    {
        return callUserCode("getConfigPanel_v1") as DisplayObject;
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

    override protected function createBackend () :EntityBackend
    {
        return new FurniBackend();
    }

    override protected function setIsBlocked (blocked :Boolean) :void
    {
        super.setIsBlocked(blocked);

        checkPerspective();
    }

    override protected function useLocationScale () :Boolean
    {
        return !_furni.isNoScale();
    }

    protected function checkPerspective () :void 
    {
        // PERSPECTIVIZATION DISABLED DURING ROOM LAYOUT REVAMP (ROBERT)
        return; // <- will abort any perspective updates

        
        if (_media == null) {
            return;
        }
        var wasPersp :Boolean = (_media is Perspectivizer);
        if (wasPersp == isPerspectivized()) {
            return;
        }

        // remove any old component
        removeChild(_media);

        var mask :DisplayObject = _media.mask;
        _media.mask = null;

        var newMedia :DisplayObject;
        if (wasPersp) {
            newMedia = Perspectivizer(_media).getSource();

        } else {
            var m :DisplayObject = _media;
// TODO: not sure if this is needed, but when we first load the loader's content
// could be null.
//            if (m is Loader) {
//                m = Loader(m).content;
//            }

            newMedia = new Perspectivizer(m);
            
            // PERSPECTIVIZATION DISABLED DURING ROOM LAYOUT REVAMP (ROBERT)

            /*
            if (parent is RoomView) {
                Perspectivizer(newMedia).updatePerspInfo(
                    RoomView(parent).layout.getPerspInfo(this, _w, _h, loc),
                    getMediaScaleX(), getMediaScaleY());
            }
            */
        }

        _media = newMedia;
        _media.mask = mask;
        addChild(_media);
    }

    override public function getLayoutHotSpot () :Point
    {
        if (_media is Perspectivizer) {
            return Perspectivizer(_media).getHotSpot();

        } else {
            return super.getLayoutHotSpot();
        }
    }

    override protected function locationUpdated () :void
    {
        updatePerspective();
        super.locationUpdated();
    }

    override protected function scaleUpdated () :void
    {
        super.scaleUpdated();
        updatePerspective();
    }

    override protected function rotationUpdated () :void
    {
        super.rotationUpdated();
        updatePerspective();
    }
    
    protected function updatePerspective () :void
    {
        if (!(_media is Perspectivizer) || !(parent is RoomView)) {
            return;
        }

        // PERSPECTIVIZATION DISABLED DURING ROOM LAYOUT REVAMP (ROBERT)
        
        /*
        var info :PerspInfo =
            RoomView(parent).layout.getPerspInfo(this, _w, _h, loc);
        Perspectivizer(_media).updatePerspInfo(
            info, getMediaScaleX(), getMediaScaleY());
        */
            
//        if (true) {
//            // draw the hotspot
//            graphics.clear();
//            graphics.beginFill(0x0099FF);
//            graphics.drawCircle(info.hotSpot.x, info.hotSpot.y, 5);
//            graphics.endFill();
//
//            // draw an outline of the perspectivized region
//            //graphics.clear();
//            graphics.lineStyle(1, 0x00FF00);
//            graphics.moveTo(info.p0.x, info.p0.y);
//            graphics.lineTo(info.p0.x, info.p0.y + info.height0);
//            graphics.lineTo(info.pN.x, info.pN.y + info.heightN);
//            graphics.lineTo(info.pN.x, info.pN.y);
//            graphics.lineTo(info.p0.x, info.p0.y);
//        }
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

    override public function getMediaScaleX () :Number
    {
        return _furni.scaleX;
    }

    override public function getMediaScaleY () :Number
    {
        return _furni.scaleY;
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

    override protected function updateMediaPosition () :void
    {
        if (_media is Perspectivizer) {
            _media.x = 0;
            _media.y = 0;

            locationUpdated();

        } else {
            super.updateMediaPosition();
        }
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
            CommandEvent.dispatch(this, RoomController.FURNI_CLICKED, _furni);
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

    override protected function addListeners (info :LoaderInfo) :void
    {
        super.addListeners(info);

        if (_loadingWatcher != null) {
            _loadingWatcher.watchLoader(info, this, (this is DecorSprite));
        }
    }

    /** The furniture data for this piece of furni. */
    protected var _furni :FurniData;

    /** The watcher for loading progress. */
    protected static var _loadingWatcher :LoadingWatcher;
}
}
