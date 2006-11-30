package com.threerings.msoy.item.client {

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.item.web.Item;

public class ItemPopupSelector extends FloatingPanel
{
    /**
     * @param callback a function that will be provided with the picked
     * item, or null.
     */
    public function ItemPopupSelector (
        ctx :MsoyContext, callback :Function, inv :InventoryWidget = null)
    {
        super(ctx, Msgs.ITEM.get("t.select_item"));
        _callback = callback;

        _inv = (inv != null) ? inv : new InventoryWidget(_ctx);
        _inv.minWidth = 350;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(_inv);
        addButtons(CANCEL_BUTTON, OK_BUTTON);

        // and create a controller that just sorta floats, eh
        new SelectorController(this, getButton(OK_BUTTON));
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

    /** The function we'll call with the picked item. */
    protected var _callback :Function;

    protected var _inv :InventoryWidget;
}
}

import mx.controls.Button;

import com.threerings.util.Controller;

import com.threerings.msoy.item.client.ItemPopupSelector;
import com.threerings.msoy.item.web.Item;

class SelectorController extends Controller
{
    public function SelectorController (panel :ItemPopupSelector, but :Button)
    {
        setControlledPanel(panel);
        _but = but;
    }

    /**
     * Handle's the InventoryWidget's ITEM_SELECTED event.
     */
    public function handleInventoryItemSelected (item :Item) :void
    {
        trace("Got item : " + item);
        _but.enabled = (item != null);
    }

    protected var _but :Button;
}
