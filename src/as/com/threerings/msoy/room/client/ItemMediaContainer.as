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
    public function ItemMediaContainer (bleepInMenu :Boolean = true)
    {
        super(bleepInMenu);
    }

    protected function setItem (item :ItemIdent) :void
    {
        _item = item;
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

    protected var _item :ItemIdent;
}
}

