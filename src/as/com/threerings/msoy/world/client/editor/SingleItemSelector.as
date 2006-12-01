package com.threerings.msoy.world.client.editor {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.containers.HBox;

import mx.controls.Button;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.client.ItemPopupSelector;
import com.threerings.msoy.item.client.ItemRenderer;
import com.threerings.msoy.item.web.Item;

public class SingleItemSelector extends HBox
{
    public function SingleItemSelector (ctx :MsoyContext)
    {
        super();

        _ctx = ctx;

        addChild(_itemHolder = new ItemRenderer());

        addChild(_add = new Button());
        _add.label = Msgs.EDITING.get("b.select_new");
        _add.addEventListener(MouseEvent.CLICK, handleClick);

        addChild(_clear = new Button());
        _clear.label = Msgs.EDITING.get("b.clear");
        _clear.addEventListener(MouseEvent.CLICK, handleClick);
        _clear.enabled = false;
    }

    public function setSelectedItem (item :Item) :void
    {
        _item = item;
        _itemHolder.data = item;

        _clear.enabled = (item != null);
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
                _ctx, setSelectedItem);
            ips.open(true, this);

        } else if (evt.target == _clear) {
            setSelectedItem(null);
        }
    }

    protected var _ctx :MsoyContext;

    protected var _itemHolder :ItemRenderer;

    protected var _item :Item;

    protected var _add :Button;
    protected var _clear :Button;
}
}
