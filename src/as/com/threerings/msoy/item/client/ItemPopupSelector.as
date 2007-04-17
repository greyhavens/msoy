package com.threerings.msoy.item.client {

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.util.CommandEvent;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.item.data.all.Item;

public class ItemPopupSelector extends FloatingPanel
{
    /**
     * @param callback a function that will be provided with the picked
     * item, or null.
     */
    public function ItemPopupSelector (
        ctx :WorldContext, callback :Function, inv :InventoryPicker = null)
    {
        super(ctx, Msgs.ITEM.get("t.select_item"));
        _callback = callback;

        _inv = (inv != null) ? inv : new InventoryPicker(_ctx);
        _inv.minWidth = 350;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(_inv);
        addButtons(CANCEL_BUTTON, OK_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        var item :Item = null;
        if (buttonId == OK_BUTTON) {
            item = _inv.getSelectedItem();
        }

        // call the callback function
        _callback(item);

        super.buttonClicked(buttonId);
    }

    override protected function handleCommand (event :CommandEvent) :void
    {
        var item :Item = event.arg as Item;
        if (event.command == InventoryPicker.ITEM_SELECTED && item != null) {
            getButton(OK_BUTTON).enabled = !item.isUsed();
        }

        super.handleCommand(event);
    }

    /** The function we'll call with the picked item. */
    protected var _callback :Function;

    protected var _inv :InventoryPicker;
}
}

