package com.threerings.msoy.world.client {

import flash.events.EventDispatcher;
import flash.events.MouseEvent;
import flash.events.TextEvent;

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
        includeInLayout = !isBackground();
        scaleUpdated();
        setLocation(furni.loc);
        configureToolTip(ctx);
    }

    override public function setEditing (editing :Boolean) :void
    {
        // clone the furni data so that we can safely modify it
        _furni = (_furni.clone() as FurniData);

        super.setEditing(editing);
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
