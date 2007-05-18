package com.threerings.msoy.item.client {

import flash.display.DisplayObjectContainer;

import mx.containers.Box;
import mx.containers.BoxDirection;

import mx.controls.Label;

import mx.core.ScrollPolicy;

import com.threerings.util.Util;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.item.data.all.Item;

/**
 * Renders an item in our inventory.
 * The item should be set to the "data" property.
 */
public class ItemRenderer extends Box
{
    /** Media files will be scaled to fit in a square of this size. */
    public static const ITEM_SIZE :Number = 100;
    
    public function ItemRenderer (direction :String = BoxDirection.HORIZONTAL)
    {
        this.direction = direction;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    override public function set data (value :Object) :void
    {
        super.data = value;

        if (processedDescriptors && !configureItem()) {
            _container.shutdown();
        }
    }

    /**
     * Return label text if we're not showing any media.
     */
    protected function configureItem () :Boolean
    {
        var mediaShown :Boolean = false;

        if (data is Item) {
            mediaShown = true;
            var item :Item = (data as Item);
            if (!Util.equals(item, _item)) {
                _item = item;

                _container.setMediaDesc(_item.getThumbnailMedia());
                _label.text = _item.name;
            }

        } else {
            _item = null;
        }

        if (data is String) {
            _label.text = (data as String);

        } else if (_item == null) {
            _label.text = Msgs.ITEM.get("m.item_none");
        }

        return mediaShown;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _container = new ScalingMediaContainer(ITEM_SIZE, ITEM_SIZE);
        var wrapper :MediaWrapper = new MediaWrapper(_container, ITEM_SIZE, ITEM_SIZE);
        addChild(wrapper);

        addChild(_label = new Label());
        _label.maxWidth = 100;

        if (data != null) {
            data = data; // re-set
        }
    }

    protected var _container :ScalingMediaContainer;
    protected var _label :Label;
    protected var _item :Item;
}
}
