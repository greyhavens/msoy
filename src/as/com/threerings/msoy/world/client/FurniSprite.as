package com.threerings.msoy.world.client {

import flash.display.BitmapData;
import flash.display.BitmapDataChannel;
import flash.display.DisplayObject;
import flash.display.Loader;

import flash.events.EventDispatcher;
import flash.events.MouseEvent;
import flash.events.TextEvent;

import flash.filters.DisplacementMapFilter;
import flash.filters.DisplacementMapFilterMode;

import flash.geom.Point;

import com.threerings.util.CommandEvent;

import com.threerings.flash.MenuUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.item.web.Item;

import com.threerings.msoy.world.data.FurniData;

public class FurniSprite extends MsoySprite
{
    /**
     * Set the LoadingWatcher that will be used to track whether any
     * FurniSprites are currently loading.
     */
    public static function setLoadingWatcher (watcher :LoadingWatcher) :void
    {
        _loadingWatcher = watcher;
        updateLoadingCount(0);
    }

    /**
     * Construct a new FurniSprite.
     */
    public function FurniSprite (furni :FurniData)
    {
        _furni = furni;
        super(furni.media, furni.getItemIdent());
        checkBackground();
        checkPerspective();
    }

    /**
     * Call the provided function when this particular sprite is done loading
     */
    public function setLoadedCallback (fn :Function) :void
    {
        _loadedCallback = fn;
    }

    override public function isIncludedInLayout () :Boolean
    {
        return !isBackground();
    }

    public function getFurniData () :FurniData
    {
        return _furni;
    }

    public function isBackground () :Boolean
    {
        return (_furni.actionType == FurniData.BACKGROUND);
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
        setup(furni.media, furni.getItemIdent());
        checkBackground();
        checkPerspective();
        scaleUpdated();
        setLocation(furni.loc);
    }

    override public function getToolTipText () :String
    {
        // clear out any residuals from the last action
        var actionData :Array = _furni.splitActionData();

        switch (_furni.actionType) {
        case FurniData.ACTION_NONE:
        case FurniData.BACKGROUND:
            // no tooltip
            return null;

        case FurniData.ACTION_URL:
            return _furni.actionData;

        case FurniData.ACTION_LOBBY_GAME:
        case FurniData.ACTION_WORLD_GAME:
            return Msgs.GENERAL.get("i.play_game", String(actionData[1]));

        case FurniData.ACTION_PORTAL:
            return Msgs.GENERAL.get("i.trav_portal", String(actionData[1]));

        default:
            log.warning("Tooltip requested for unhandled furni action type " +
                "[actionType=" + _furni.actionType +
                ", actionData=" + _furni.actionData + "].");
            return null;
        }
    }

    /**
     * If we're a portal furniture, called to animate a player entering
     * or leaving.
     */
    public function wasTraversed (entering :Boolean) :void
    {
        // TODO: figure out which messages we want to send to doors
        callUserCode("action_v1", entering ? "bodyEntered" : "bodyLeft");
    }

    override protected function createBackend () :EntityBackend
    {
        return new FurniBackend();
    }

    protected function checkPerspective () :void 
    {
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

            if (parent is AbstractRoomView) {
                Perspectivizer(newMedia).updatePerspInfo(
                    AbstractRoomView(parent).getPerspInfo(this, _w, _h, loc),
                    getMediaScaleX(), getMediaScaleY());
            }
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

    protected function updatePerspective () :void
    {
        if (!(_media is Perspectivizer) || !(parent is AbstractRoomView)) {
            return;
        }

        var info :PerspInfo =
            AbstractRoomView(parent).getPerspInfo(this, _w, _h, loc);
        Perspectivizer(_media).updatePerspInfo(
            info, getMediaScaleX(), getMediaScaleY());

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

    override public function setEditing (editing :Boolean) :void
    {
        // clone the furni data so that we can safely modify it
        _furni = (_furni.clone() as FurniData);

        super.setEditing(editing);

        checkBackground();
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

    override public function getMaxContentWidth () :int
    {
        return 2000;
    }

    override public function getMaxContentHeight () :int
    {
        return 1000;
    }

    // documentation inherited
    override public function hasAction () :Boolean
    {
        switch (_furni.actionType) {
        case FurniData.ACTION_NONE:
        case FurniData.BACKGROUND:
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

    // documentation inherited
    override public function mouseClick (event :MouseEvent) :void
    {
        CommandEvent.dispatch(this, RoomController.FURNI_CLICKED, _furni);
    }

    /**
     * Configure any layout changes that may be different when we're
     * 'background' or not.
     */
    protected function checkBackground () :void
    {
        alpha = (_editing && isBackground()) ? .65 : 1;
    }

    override protected function startedLoading () :void
    {
        updateLoadingCount(1);
        super.startedLoading();
    }

    override protected function stoppedLoading () :void
    {
        if (_loadedCallback != null) {
            _loadedCallback();
        }

        updateLoadingCount(-1);
        super.stoppedLoading();
    }

    /**
     * Update the number of FurniSprites that are currently loading.
     */
    protected static function updateLoadingCount (delta :int) :void
    {
        _loadingCount += delta;
        _loadingWatcher.setLoading(_loadingCount > 0);
    }

    /** The furniture data for this piece of furni. */
    protected var _furni :FurniData;

    /** A function we call when we've finished loading. */
    protected var _loadedCallback :Function;

    /** The number of furni items currently loading. */
    protected static var _loadingCount :int = 0;

    /** The watcher for loading progress. */
    protected static var _loadingWatcher :LoadingWatcher;
}
}
