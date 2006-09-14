package com.threerings.msoy.item.client {

import flash.display.DisplayObjectContainer;

import mx.containers.VBox;

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
public class ItemRenderer extends VBox
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

    override protected function measure () :void
    {
        measuredWidth = 300;
        measuredHeight = 250;
    }

    protected function recheckItem () :void
    {
        var item :Item = (data as Item);
        if (!Util.equals(item, _item)) {
            _item = item;

            if (_item == null) {
                _container.shutdown();
                _label.text = "";

            } else {
                _container.setMedia(_item.getThumbnailPath());
                _label.text = _item.getDescription();
            }
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
        _container = new ScalingMediaContainer(250, 250);
        /*
        _container.maxWidth = 250;
        _container.maxHeight = 200;
        */
        addChild(_container);

        addChild(_label = new Label());
        _label.maxHeight = 50;
        _label.maxWidth = 250;
    }

    protected var _container :MediaContainer;
    protected var _label :Label;
    protected var _item :Item;
}
}
