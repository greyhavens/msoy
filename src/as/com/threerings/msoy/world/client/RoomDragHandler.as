//
// $Id$

package com.threerings.msoy.world.client {

import mx.events.DragEvent;
import mx.managers.DragManager;

import com.threerings.msoy.item.client.InventoryPicker;

/**
 * Handles some standard bits when dragging an item into the room view.
 */
public class RoomDragHandler
{
    public function RoomDragHandler (roomView :RoomView) :void
    {
        _roomView = roomView;
        _roomView.addEventListener(DragEvent.DRAG_ENTER, dragEnterHandler);
        _roomView.addEventListener(DragEvent.DRAG_EXIT, dragExitHandler);
        _roomView.addEventListener(DragEvent.DRAG_OVER, dragOverHandler);
    }

    public function unbind () :void
    {
        _roomView.removeEventListener(DragEvent.DRAG_ENTER, dragEnterHandler);
        _roomView.removeEventListener(DragEvent.DRAG_EXIT, dragExitHandler);
        _roomView.removeEventListener(DragEvent.DRAG_OVER, dragOverHandler);
    }

    protected function dragEnterHandler (event :DragEvent) :void
    {
        if (event.isDefaultPrevented()) {
            return;
        }

        if (null != InventoryPicker.dragItem(event)) {
            DragManager.acceptDragDrop(_roomView);
            DragManager.showFeedback(DragManager.MOVE);
        } else {
            DragManager.showFeedback(DragManager.NONE);
        }
    }

    protected function dragOverHandler (event :DragEvent) :void
    {
        if (event.isDefaultPrevented()) {
            return;
        }

        if (null != InventoryPicker.dragItem(event)) {
            DragManager.showFeedback(DragManager.MOVE);
        } else {
            DragManager.showFeedback(DragManager.NONE);
        }
    }

    protected function dragExitHandler (event :DragEvent) :void
    {
        if (event.isDefaultPrevented()) {
            return;
        }

        DragManager.showFeedback(DragManager.NONE);
    }

    protected var _roomView :RoomView;
}
}
