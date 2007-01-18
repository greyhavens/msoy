//
// $Id$

package com.threerings.msoy.world.client {

import mx.core.Container;
import mx.events.DragEvent;
import mx.managers.DragManager;

import com.threerings.msoy.item.client.InventoryPicker;

/**
 * Handles some standard bits when dragging an item into the room view.
 */
public class RoomDragHandler
{
    /**
     * @param roomContainer The container holding the room, which
     * may be obtained with ctx.getTopPanel().getPlaceContainer()
     */
    public function RoomDragHandler (roomContainer :Container) :void
    {
        _container = roomContainer;
        _container.addEventListener(DragEvent.DRAG_ENTER, dragEnterHandler);
        _container.addEventListener(DragEvent.DRAG_EXIT, dragExitHandler);
        _container.addEventListener(DragEvent.DRAG_OVER, dragOverHandler);
    }

    public function unbind () :void
    {
        _container.removeEventListener(DragEvent.DRAG_ENTER, dragEnterHandler);
        _container.removeEventListener(DragEvent.DRAG_EXIT, dragExitHandler);
        _container.removeEventListener(DragEvent.DRAG_OVER, dragOverHandler);
    }

    protected function dragEnterHandler (event :DragEvent) :void
    {
        if (event.isDefaultPrevented()) {
            return;
        }

        if (null != InventoryPicker.dragItem(event)) {
            DragManager.acceptDragDrop(_container);
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

    protected var _container :Container;
}
}
