//
// $Id$

package com.threerings.msoy.client {

import mx.events.DragEvent;

import mx.core.Container;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.item.client.InventoryPicker;
import com.threerings.msoy.item.web.Item;

import com.threerings.msoy.world.client.PetService;
import com.threerings.msoy.world.client.RoomDragHandler;
import com.threerings.msoy.world.client.RoomView;

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

/**
 * Displays a list of the player's pets and allows them to be "called" into the current room.
 */
public class PetsDialog extends FloatingPanel
{
    public function PetsDialog (ctx :MsoyContext)
    {
        super(ctx, Msgs.GENERAL.get("t.pets"));

        _roomContainer = ctx.getTopPanel().getPlaceContainer();
        _roomContainer.addEventListener(DragEvent.DRAG_DROP, dragDropHandler);
        _roomDragger = new RoomDragHandler(_roomContainer);
        _pets = new InventoryPicker(_ctx, Item.PET, true);
        open(false);
    }

    // from FloatingPanel
    override public function close () :void
    {
        super.close();
        _roomContainer.removeEventListener(DragEvent.DRAG_DROP, dragDropHandler);
        _roomDragger.unbind();
    }

    // from UIComponent
    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(MsoyUI.createLabel(Msgs.GENERAL.get("l.pets_tip")));

        _pets.percentWidth = 100;
        addChild(_pets);

        addButtons(OK_BUTTON);
    }

    protected function dragDropHandler (event :DragEvent) :void
    {
        if (event.isDefaultPrevented()) {
            return;
        }

        var item :Item = InventoryPicker.dragItem(event);
        var svc :PetService = (_ctx.getClient().requireService(PetService) as PetService);
        svc.callPet(_ctx.getClient(), item.itemId,
                    new ReportingListener(_ctx, "general", null, "m.pet_called"));
    }

    protected var _roomContainer :Container;
    protected var _roomDragger :RoomDragHandler;
    protected var _pets :InventoryPicker;
}
}
