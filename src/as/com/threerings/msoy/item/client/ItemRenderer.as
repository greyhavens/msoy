package com.threerings.msoy.item.client {

import flash.display.DisplayObjectContainer;

import mx.containers.HBox;

import mx.controls.Label;

import mx.core.ScrollPolicy;

import com.threerings.util.MediaContainer;
import com.threerings.util.Util;

import com.threerings.msoy.ui.ScalingMediaContainer;

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

    override public function validateDisplayList () :void
    {
        super.validateDisplayList();
        recheckItem();
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p == null && data != null) {
            data = null;
            recheckItem();
        }
    }

//    override protected function measure () :void
//    {
//        measuredWidth = 300;
//        measuredHeight = 250;
//    }

    protected function recheckItem () :void
    {
        if (data is Item) {
            var item :Item = (data as Item);
            if (!Util.equals(item, _item)) {
                _item = item;

                _container.setMedia(_item.getThumbnailPath());
                _label.text = _item.getDescription();
///                validateNow();
            }

        } else if (_item != null) {
            _container.shutdown();
            _item = null;
        }

        if (data is String) {
            _label.text = (data as String);
        }
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
        _container = new ScalingMediaContainer(100, 100);
        /*
        _container.maxWidth = 250;
        _container.maxHeight = 200;
        */
        addChild(_container);

        addChild(_label = new Label());
        _label.maxWidth = 200;
    }

    protected var _container :MediaContainer;
    protected var _label :Label;
    protected var _item :Item;
}
}
