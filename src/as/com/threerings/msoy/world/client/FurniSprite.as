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
        super(furni.media);
        includeInLayout = !isBackground();

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

    public function update (ctx :MsoyContext, furni :FurniData) :void
    {
        _furni = furni;
        setup(furni.media);
        checkBackground();
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
        sendMessage("action", entering ? "bodyEntered" : "bodyLeft");
    }

    public function addPersp () :void
    {
        if (_media is UIComponent) {
            removeChild(_media);
        } else {
            rawChildren.removeChild(_media);
        }

        var newMedia :DisplayObject;
        if (_media is Perspectivizer) {
            newMedia = Perspectivizer(_media).getSource();

        } else {
            var m :DisplayObject = _media;
            if (m is Loader) {
                m = Loader(m).content;
            }

            newMedia = new Perspectivizer(m,
                AbstractRoomView(parent).getPerspInfo(this, _w, _h, loc),
                getMediaScaleX(), getMediaScaleY());
        }

        _media = newMedia;
        if (_media is UIComponent) {
            addChild(_media);
        } else {
            rawChildren.addChild(_media);
        }

        scaleUpdated();
    }

    override protected function locationUpdated () :void
    {
        super.locationUpdated();
        checkPerspective();
    }

    override protected function scaleUpdated () :void
    {
        super.scaleUpdated();
        checkPerspective();
    }

    protected function checkPerspective () :void
    {
        if (!(_media is Perspectivizer) || !(parent is AbstractRoomView)) {
            return;
        }

        var arr :Array =
            AbstractRoomView(parent).getPerspInfo(this, _w, _h, loc);
        Perspectivizer(_media).updatePerspInfo(
            arr, getMediaScaleX(), getMediaScaleY());

//        var sub :Array = (arr[arr.length - 1] as Array);
//
//        graphics.clear();
//        graphics.lineStyle(1, 0xFF0000);
//        graphics.moveTo(Number(sub[0]), Number(sub[1]));
//        for each (sub in arr) {
//            graphics.lineTo(Number(sub[0]), Number(sub[1]));
//        }
    }

//    {
//        var pinchLeft :Number = .1;
//        var pinchRight :Number = 1;
//
//        var maxVal :Number = (127 / 256); // max positive displacement
//        var maxScaleDiff :Number = _h * (1 - Math.min(pinchLeft, pinchRight));
//        var maxJump :Number = maxScaleDiff / maxVal;
//
//        var filter :DisplacementMapFilter = new DisplacementMapFilter();
//        filter.alpha = 0;
//        filter.componentX = BitmapDataChannel.RED;
//        filter.componentY = BitmapDataChannel.BLUE;
//        filter.mode = DisplacementMapFilterMode.COLOR;
//        filter.scaleX = 1;
//        filter.scaleY = maxJump;
//
//        var map :BitmapData = new BitmapData(_w, _h, false, 0xFFFFFF);
//        var ww :Number = _w - 1;
//        for (var xx :int = 0; xx < _w; xx++) {
//            var rightness :Number = (xx / ww);
//            var scaleHere :Number = (rightness * pinchRight) +
//                ((1 - rightness) * pinchLeft);
//            var srcHeight :Number = _h / scaleHere;
//            var start :Number = (_h - srcHeight) / 2;
//            for (var yy :int = 0; yy < _h; yy++) {
//                var srcYY :Number = (yy / _h) * srcHeight + start;
//                var val :Number = (srcYY - yy) / maxJump;
//                val = Math.max(-maxVal, Math.min(maxVal, val));
//                var jump :uint = 128 + (val * 256);
//                map.setPixel(xx, yy, uint(((uint(128) << 16) | jump)));
//            }
//        }
//
//        filter.mapBitmap = map;
//
//        this.filters = [ filter ];
//    }

    override public function setEditing (editing :Boolean) :void
    {
        // clone the furni data so that we can safely modify it
        _furni = (_furni.clone() as FurniData);

        super.setEditing(editing);

        checkBackground();

        if (editing) {
            // we don't want a tooltip while editing
            toolTip = null;

        } else {
            // TEMP: to undo perspective
//            filters = [];
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

    override public function get maxContentWidth () :int
    {
        return 2000;
    }

    override public function get maxContentHeight () :int
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
    override protected function mouseClick (event :MouseEvent) :void
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

        case FurniData.ACTION_GAME:
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

    override protected function addContentListeners (
        dispatch :EventDispatcher) :void
    {
        super.addContentListeners(dispatch);

        dispatch.addEventListener("msoyLoc", handleInterfaceMsoyLoc);
        dispatch.addEventListener("query", handleInterfaceQuery);
    }

    override protected function removeContentListeners (
        dispatch :EventDispatcher) :void
    {
        super.removeContentListeners(dispatch);

        dispatch.removeEventListener("msoyLoc", handleInterfaceMsoyLoc);
        dispatch.removeEventListener("query", handleInterfaceQuery);
    }

    protected function handleInterfaceMsoyLoc (event :TextEvent) :void
    {
        if (_editing) {
            return; // do not allow movement during editing
        }
        var loc :Array = event.text.split(";");
        setLocation(loc.map(
            function (item :*, index :int, array :Array) :Number {
                return Math.min(1, Math.max(0, Number(item)));
            }));
    }

    protected function handleInterfaceQuery (event :TextEvent) :void
    {
        switch (event.text) {
        case "msoyLoc":
            sendMessage("result",
                "" + loc.x + ";" + loc.y + ";" + loc.z + ";" + loc.orient);
            break;

        default:
            log.warning("Unknown query from furniture: " + event.text);
            break;
        }
    }

    /** The furniture data for this piece of furni. */
    protected var _furni :FurniData;
}
}
