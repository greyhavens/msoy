//
// $Id$

package com.threerings.msoy.world.client.editor {

import mx.events.DragEvent;

import mx.core.Container;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.item.client.InventoryPicker;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.client.RoomDragHandler;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.updates.FurniUpdateAction;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;

/**
 * Displays a list of the player's pets and allows them to be "called" into the current room.
 */
public class FurniAddDialog extends FloatingPanel
{
    public function FurniAddDialog (ctx :WorldContext, roomView :RoomView, scene :MsoyScene)
    {
        super(ctx, Msgs.GENERAL.get("t.add_furni"));

        _roomView = roomView;
        _scene = scene;
        _roomContainer = ctx.getTopPanel().getPlaceContainer();
        _roomContainer.addEventListener(DragEvent.DRAG_DROP, dragDropHandler);
        _roomDragger = new RoomDragHandler(_roomContainer);
        _furni = new InventoryPicker(
            _ctx, [ Item.PHOTO, Item.DOCUMENT, Item.FURNITURE, Item.GAME, Item.VIDEO ], null);
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

        addChild(MsoyUI.createLabel(Msgs.GENERAL.get("l.add_furni_tip")));

        _furni.percentWidth = 100;
        addChild(_furni);

        addButtons(OK_BUTTON);

        this.width = 300; 
    }

    protected function dragDropHandler (event :DragEvent) :void
    {
        if (event.isDefaultPrevented()) {
            return;
        }

        var item :Item = InventoryPicker.dragItem(event);
        var cloc :ClickLocation =
            _roomView.layout.pointToFurniLocation(event.stageX, event.stageY);

        addFurni(item, cloc.loc);
    }

    /**
     * Add a new piece of furni to the scene.
     */
    protected function addFurni (item :Item, loc :MsoyLocation) :void
    {
        // create a generic furniture descriptor
        var furni :FurniData = new FurniData();
        furni.id = _scene.getNextFurniId(0);
        furni.itemType = item.getType();
        furni.itemId = item.itemId;
        furni.media = item.getFurniMedia();
        furni.loc = loc;
        configureFurniAction(furni, item);

        _roomView.getRoomController().applyUpdate(new FurniUpdateAction(_ctx, null, furni));
    }

    /**
     * Configure the default action for furni constructed from the specified object.
     */
    protected function configureFurniAction (furni :FurniData, item :Item) :void
    {
        if (item is Game) {
            var game :Game = (item as Game);
            furni.actionType = game.isInWorld() ?
                FurniData.ACTION_WORLD_GAME : FurniData.ACTION_LOBBY_GAME;
            furni.actionData = String(game.getPrototypeId()) + ":" + game.name;
        }
    }

    protected var _scene :MsoyScene;
    protected var _roomView :RoomView;
    protected var _roomContainer :Container;
    protected var _roomDragger :RoomDragHandler;
    protected var _furni :InventoryPicker;
}
}
