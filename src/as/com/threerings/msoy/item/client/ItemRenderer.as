package com.threerings.msoy.item.client {

import flash.display.DisplayObjectContainer;

import mx.containers.HBox;

import mx.controls.Label;

import mx.core.ScrollPolicy;

import com.threerings.util.MediaContainer;
import com.threerings.util.Util;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.ui.ScalingMediaBox;

import com.threerings.msoy.item.web.Item;

/**
 * Renders an item in our inventory.
 * The item should be set to the "data" property.
 */
public class ItemRenderer extends HBox
{
    public function ItemRenderer ()
    {
        super();
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

                _container.setMedia(_item.getThumbnailPath());
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

/*
        var scrollBox :VBox = new VBox();
        scrollBox.maxWidth = 250;
        scrollBox.maxHeight = 200;

        addChild(scrollBox);
        scrollBox.addChild(_container = new MediaContainer());
*/
        _container = new ScalingMediaBox(100, 100);
        /*
        _container.maxWidth = 250;
        _container.maxHeight = 200;
        */
        addChild(_container);

        addChild(_label = new Label());
        _label.maxWidth = 200;

        if (data != null) {
            data = data; // re-set
        }
    }

    protected var _container :ScalingMediaBox;
    protected var _label :Label;
    protected var _item :Item;
}
}
