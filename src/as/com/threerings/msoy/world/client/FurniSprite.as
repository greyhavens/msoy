package com.threerings.msoy.world.client {

import flash.display.BitmapData;
import flash.display.BitmapDataChannel;

import flash.events.EventDispatcher;
import flash.events.MouseEvent;
import flash.events.TextEvent;

import flash.filters.DisplacementMapFilter;
import flash.filters.DisplacementMapFilterMode;

import com.threerings.mx.events.CommandEvent;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.world.data.FurniData;

public class FurniSprite extends MsoySprite
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
        var pinchLeft :Number = .1;
        var pinchRight :Number = 1;

        var maxVal :Number = (127 / 256); // max positive displacement
        var maxScaleDiff :Number = _h * (1 - Math.min(pinchLeft, pinchRight));
        var maxJump :Number = maxScaleDiff / maxVal;

        var filter :DisplacementMapFilter = new DisplacementMapFilter();
        filter.alpha = 0;
        filter.componentX = BitmapDataChannel.RED;
        filter.componentY = BitmapDataChannel.BLUE;
        filter.mode = DisplacementMapFilterMode.COLOR;
        filter.scaleX = 1;
        filter.scaleY = maxJump;

        var map :BitmapData = new BitmapData(_w, _h, false, 0xFFFFFF);
        var ww :Number = _w - 1;
        for (var xx :int = 0; xx < _w; xx++) {
            var rightness :Number = (xx / ww);
            var scaleHere :Number = (rightness * pinchRight) +
                ((1 - rightness) * pinchLeft);
            var srcHeight :Number = _h / scaleHere;
            var start :Number = (_h - srcHeight) / 2;
            for (var yy :int = 0; yy < _h; yy++) {
                var srcYY :Number = (yy / _h) * srcHeight + start;
                var val :Number = (srcYY - yy) / maxJump;
                val = Math.max(-maxVal, Math.min(maxVal, val));
                var jump :uint = 128 + (val * 256);
                map.setPixel(xx, yy, uint(((uint(128) << 16) | jump)));
            }
        }

        filter.mapBitmap = map;

        this.filters = [ filter ];
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

        } else {
            // TEMP: to undo perspective
            filters = [];
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
            toolTip = ctx.xlate(null, "i.play_game", String(actionData[1]));
            break;

        case FurniData.ACTION_PORTAL:
            toolTip = ctx.xlate(null, "i.trav_portal");
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

        dispatch.addEventListener("msoyLoc", function (event :TextEvent) :void {
            if (_editing) {
                return; // do not allow movement during editing
            }
            var loc :Array = event.text.split(",");
            setLocation(loc.map(
                function (item :*, index :int, array :Array) :Number {
                    return Number(item);
                }));
        });
    }

    /** The furniture data for this piece of furni. */
    protected var _furni :FurniData;
}
}
