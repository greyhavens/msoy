package com.threerings.msoy.world.client.editor {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Button;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.item.client.ItemPopupSelector;
import com.threerings.msoy.item.client.ItemRenderer;
import com.threerings.msoy.item.client.InventoryPicker;
import com.threerings.msoy.item.data.all.Item;

public class SingleItemSelector extends VBox
{
    /** A function to be invoked when the selection changes. */
    public var selectionChanged :Function;

    public function SingleItemSelector (
        ctx :WorldContext, soleType :int = Item.NOT_A_TYPE)
    {
        super();

        _ctx = ctx;
        _soleType = soleType;

        addChild(_itemHolder = new ItemRenderer());

        var hbox :HBox = new HBox();

        hbox.addChild(_add = new Button());
        _add.label = Msgs.EDITING.get("b.select_new");
        _add.addEventListener(MouseEvent.CLICK, handleClick);

        hbox.addChild(_clear = new Button());
        _clear.label = Msgs.EDITING.get("b.clear");
        _clear.addEventListener(MouseEvent.CLICK, handleClick);
        _clear.enabled = false;

        addChild(hbox);
    }

    public function setSelectedItem (item :Item) :void
    {
        _item = item;
        _itemHolder.data = item;

        _clear.enabled = (item != null);

        if (selectionChanged != null) {
            try {
                selectionChanged();
            } catch (er :Error) {
                // nada
            }
        }
    }

    public function getSelectedItem () :Item
    {
        return _item;
    }

    protected function handleClick (evt :MouseEvent) :void
    {
        if (evt.target == _add) {
            // TODO: use the same inventory widget for all editing ops?
            var ips :ItemPopupSelector = new ItemPopupSelector(
                _ctx,
                function (item :Item) :void {
                    if (item != null) {
                        setSelectedItem(item);
                    }
                },
                new InventoryPicker(_ctx, [ _soleType ]));
            ips.open(true, this);

        } else if (evt.target == _clear) {
            setSelectedItem(null);
        }
    }

    protected var _ctx :WorldContext;

    protected var _itemHolder :ItemRenderer;

    protected var _item :Item;

    protected var _soleType :int;

    protected var _add :Button;
    protected var _clear :Button;
}
}
