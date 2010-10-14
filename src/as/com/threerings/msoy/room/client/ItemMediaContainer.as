//
// $Id: MsoySprite.as 19383 2010-10-13 20:48:25Z zell $

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;

import com.threerings.util.CommandEvent;

import com.threerings.media.VideoPlayer;

import com.threerings.msoy.ui.MsoyVideoDisplay;
import com.threerings.msoy.ui.DataPackMediaContainer;

import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.client.MsoyController;

/**
 * A tiny layer on top of a DataPackMediaContainer that knows it's representing media
 * for an item.
 */
public class ItemMediaContainer extends DataPackMediaContainer
{
    public function ItemMediaContainer (
        bleepInMenu :Boolean = true, suppressHitTestPoint :Boolean = false)
    {
        super(bleepInMenu);

        _suppressHitTestPoint = suppressHitTestPoint;
    }

    public function setItem (item :ItemIdent) :void
    {
        _item = item;
    }

    public function setSuppressHitTestPoint (suppress :Boolean) :void
    {
        _suppressHitTestPoint = suppress;
    }

    public function setMaxContentDimensions (width :int, height :int) :void
    {
        _maxWidth = width;
        _maxHeight = height;
    }

    // documentation inherited
    override public function hitTestPoint (
        x :Number, y :Number, shapeFlag :Boolean = false) :Boolean
    {
        if (_suppressHitTestPoint) {
            return false;
        }
        return super.hitTestPoint(x, y, shapeFlag);
    }


    /** @inheritDoc */
    // from MediaContainer
    override public function getMediaScaleX () :Number
    {
        // use a fixed scale for blocked media
        return isBlocked() ? 1 : _spriteMediaScaleX;
    }

    /** @inheritDoc */
    // from MediaContainer
    override public function getMediaScaleY () :Number
    {
        // use a fixed scale for blocked media
        return isBlocked() ? 1 : _spriteMediaScaleY;
    }

    /**
     * Set the media scale to use when we are not displaying a blocked state.
     */
    public function setSpriteMediaScale (scaleX :Number, scaleY :Number) :void
    {
        _spriteMediaScaleX = scaleX;
        _spriteMediaScaleY = scaleY;
    }

    // from MsoySprite
    override public function getMaxContentWidth () :int
    {
        return _maxWidth;
    }

    // from MsoySprite
    override public function getMaxContentHeight () :int
    {
        return _maxHeight;
    }

    override protected function handleUncaughtErrors (event :*) :void
    {
        // override the version in MsoyMediaContainer so that we can also log the ident.
        log.info("Uncaught Error", "item", _item, "media", _desc, event);
    }

    override protected function createVideoUI (player :VideoPlayer) :DisplayObject
    {
        // here, we assume that the ItemItem is configured prior to the MediaDesc. Should be true.
        return new MsoyVideoDisplay(player, (_item == null) ? null : handleViewItem);
    }

    /**
     * Handle the "comment" button on the video player.
     */
    protected function handleViewItem () :void
    {
        CommandEvent.dispatch(this, MsoyController.VIEW_ITEM, _item);
    }

    protected var _suppressHitTestPoint :Boolean;

    protected var _item :ItemIdent;

    protected var _maxWidth :int = int.MAX_VALUE;
    protected var _maxHeight :int = int.MAX_VALUE;

    /** The media scale to use when we are not blocked. */
    protected var _spriteMediaScaleX :Number = 1.0;

    /** The media scale to use when we are not blocked. */
    protected var _spriteMediaScaleY :Number = 1.0;
}
}

