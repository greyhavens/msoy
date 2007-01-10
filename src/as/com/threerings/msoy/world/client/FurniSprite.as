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

import mx.core.UIComponent;

import com.threerings.util.MenuUtil;

import com.threerings.mx.events.CommandEvent;

import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.item.web.Item;

import com.threerings.msoy.world.data.FurniData;

public class FurniSprite extends MsoySprite
    implements ContextMenuProvider
{
    public function FurniSprite (ctx :MsoyContext, furni :FurniData)
    {
        _furni = furni;
        super(furni.media, furni.getIdent());
        checkBackground();
        checkPerspective();

        configureToolTip(ctx);
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

    public function update (ctx :MsoyContext, furni :FurniData) :void
    {
        _furni = furni;
        setup(furni.media, furni.getIdent());
        checkBackground();
        checkPerspective();
        scaleUpdated();
        setLocation(furni.loc);
        configureToolTip(ctx);
    }

    // from ContextMenuProvider
    public function populateContextMenu (menuItems :Array) :void
    {
        if (_furni.itemType != Item.NOT_A_TYPE) {
            menuItems.unshift(MenuUtil.createControllerMenuItem(
                Msgs.GENERAL.get("b.view_item"), MsoyController.VIEW_ITEM,
                [ Item.getTypeName(_furni.itemType), _furni.itemId ]));
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
        if (_media is UIComponent) {
            removeChild(_media);
        } else {
            rawChildren.removeChild(_media);
        }

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
        if (_media is UIComponent) {
            addChild(_media);
        } else {
            rawChildren.addChild(_media);
        }
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

        if (editing) {
            // we don't want a tooltip while editing
            toolTip = null;
        }
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

    override public function isInteractive () :Boolean
    {
        return hasAction();
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
    override protected function getHoverColor () :uint
    {
        switch (_furni.actionType) {
        case FurniData.ACTION_PORTAL:
            return 0xe04040; // red

        default:
            return 0xe0e040; // yellow
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
        includeInLayout = !isBackground();
        alpha = (_editing && isBackground()) ? .65 : 1;
    }

    /**
     * Do any setup required for the furniture's tooltip.
     */
    protected function configureToolTip (ctx :MsoyContext) :void
    {
        // clear out any residuals from the last action
        toolTip = null;
        var actionData :Array = _furni.splitActionData();

        switch (_furni.actionType) {
        case FurniData.ACTION_NONE:
        case FurniData.BACKGROUND:
            // do nothing
            break;

        case FurniData.ACTION_URL:
            toolTip = _furni.actionData;
            break;

        case FurniData.ACTION_LOBBY_GAME:
        case FurniData.ACTION_WORLD_GAME:
            toolTip = Msgs.GENERAL.get("i.play_game", String(actionData[1]));
            break;

        case FurniData.ACTION_PORTAL:
            toolTip = Msgs.GENERAL.get("i.trav_portal", String(actionData[1]));
            break;

        default:
            log.warning("Tooltip requested for unhandled furni action type " +
                "[actionType=" + _furni.actionType +
                ", actionData=" + _furni.actionData + "].");
            break;
        }
    }

    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["getLocation_v1"] = getLocation_v1;
        o["setLocation_v1"] = setLocation_v1;
    }

    protected function getLocation_v1 () :Array
    {
        return [ loc.x, loc.y, loc.z, loc.orient ];
    }

    protected function setLocation_v1 (loc :Array) :void
    {
        if (_editing) {
            return; // do not allow movement during editing
        }
        if (loc == null || loc.length < 3) {
            return; // don't fuck up
        }
        loc.splice(3); // lop off any extra crap
        loc = loc.map(function (item :*, index :int, array :Array) :Number {
            var n :Number = Number(item);
            if (isNaN(n)) {
                return .5;
            }
            return Math.min(1, Math.max(0, n));
        });
        setLocation(loc);
    }

    /** The furniture data for this piece of furni. */
    protected var _furni :FurniData;
}
}
