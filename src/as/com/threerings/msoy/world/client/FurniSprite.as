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

    public function addPersp () :void
    {
        var pinchLeft :Number = .5;
        var pinchRight :Number = 1;

        var map :BitmapData = new BitmapData(_w, _h, false, 0xFFFFFF);
        for (var xx :int = 0; xx < _w; xx++) {
            var ww :int = _w - 1;
            var rightness :Number = (xx / _w);
            var scaleHere :Number = (rightness * pinchRight) +
                ((1 - rightness) * pinchLeft);
            Log.testing("scaleHere: " + scaleHere);
            var heightHere :Number = _h * scaleHere;
            for (var yy :int = 0; yy < _h; yy++) {
                map.setPixel(xx, yy, makePixel(yy, scaleHere, heightHere));
            }
        }

        var filter :DisplacementMapFilter = new DisplacementMapFilter();
        filter.alpha = 0;
        filter.componentX = BitmapDataChannel.RED;
        filter.componentY = BitmapDataChannel.BLUE;
        filter.mapBitmap = map;
        filter.mode = DisplacementMapFilterMode.COLOR;
        filter.scaleX = 1;
        filter.scaleY = (_h * (1 - Math.min(pinchLeft, pinchRight)));

        this.filters = [ filter ];
    }

    protected function makePixel (
        yy :Number, scaleHere :Number, heightHere :Number) :uint
    {
        var srcHeight :Number = _h / scaleHere;
        var start :Number = (_h - srcHeight) / 2;
        var srcYY :Number = (yy / _h) * srcHeight + start;

        var jump :uint = Math.min(255, Math.max(0,
            ((srcYY - yy) / (_h * .5)) * 256 + 128));

        return (uint(128) << 16) | jump;
    }

    override public function setEditing (editing :Boolean) :void
    {
        // clone the furni data so that we can safely modify it
        _furni = (_furni.clone() as FurniData);

        super.setEditing(editing);

        checkBackground();

        // turn the filters back off
        if (!editing) {
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
        return 0xe0e040; // yellow
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

        switch (_furni.actionType) {
        case FurniData.ACTION_NONE:
        case FurniData.BACKGROUND:
            // do nothing
            break;

        case FurniData.ACTION_URL:
            toolTip = _furni.actionData;
            break;

        case FurniData.ACTION_GAME:
            toolTip = ctx.xlate(null, "i.play_game");
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
