package com.threerings.msoy.world.client.editor {

import flash.events.Event;

import mx.core.ClassFactory;

import mx.controls.ComboBox;

import mx.collections.ArrayCollection;

import com.threerings.util.Util;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.client.ItemPopupSelector;
import com.threerings.msoy.item.client.ItemRenderer;
import com.threerings.msoy.item.web.Item;

public class SingleItemSelector extends ComboBox
{
    public function SingleItemSelector (ctx :MsoyContext)
    {
        super();

        _ctx = ctx;

        itemRenderer = new ClassFactory(ItemRenderer);

        _collection.addItem(Msgs.EDITING.get("m.none"));
        _collection.addItem(Msgs.EDITING.get("b.select_new"));

        dataProvider = _collection;

        addEventListener(Event.CHANGE, handleSelectionChange);
    }

    override public function set selectedItem (item :Object) :void
    {
        // if the new item is equals() to an existing item, select the
        // existing item
        for (var ii :int = 0; ii < _collection.length; ii++) {
            if (Util.equals(item, _collection[ii])) {
                super.selectedItem = _collection[ii];
                return;
            }
        }

        // otherwise, add the new item prior to the end..
        _collection.addItemAt(item, _collection.length - 1);
        super.selectedItem = item;
    }

    protected function handleSelectionChange (evt :Event) :void
    {
        // if they ever select the last item, they need to select more..
        if (selectedIndex != _collection.length - 1) {
            _lastSelectedIndex = selectedIndex;
            return;
        }

        // TODO: use the same inventory widget for all editing ops?
        var ips :ItemPopupSelector = new ItemPopupSelector(
            _ctx, newItemSelected);
        ips.open(true, this);
    }

    /**
     * A callback passed to the ItemPopupSelector.
     */
    protected function newItemSelected (item :Item) :void
    {
        if (item == null) {
            // nothing new selected, revert to the last selected item
            selectedIndex = _lastSelectedIndex;

        } else {
            // add the new item..
            selectedItem = item;
        }
    }

    protected var _ctx :MsoyContext;

    /** The collection what stores the goods. */
    protected var _collection :ArrayCollection = new ArrayCollection();

    protected var _lastSelectedIndex :int;
}
}
