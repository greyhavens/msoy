//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.events.Event;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.core.Container;

import com.threerings.flex.CommandButton;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.room.data.FurniData;

/**
 * Basic panel that displays apply and cancel buttons, and sends updates to the controller.
 */
public class BasePanel extends VBox
{
    public function BasePanel (controller :RoomEditorController)
    {
        _controller = controller;
        this.percentWidth = 100;
    }

    /**
     * Updates the UI from furni data. Data can be null, in which case the panel
     * will be disabled. */
    public function updateDisplay (data :FurniData) :void
    {
        if (data == null) {
            this.enabled = false;
            _furniData = null;

        } else {
            this.enabled = true;
            _furniData = data.clone() as FurniData;
        }

        // whatever edits were pending, they will be gone by the time the subclass is done
        // with this update. so just disable the buttons.
        setChanged(false);
    }

    /** Subclasses can call this function to create a box filled in with panel buttons. */
    protected function makePanelButtons () :Container
    {
        _applyButton = new CommandButton(Msgs.EDITING.get("b.apply_changes"), applyChanges);
        _applyButton.styleName = "roomEditPanelButton";
        _applyButton.height = 20;
        _cancelButton = new CommandButton(Msgs.EDITING.get("b.revert_changes"), revertChanges);
        _cancelButton.styleName = "roomEditPanelButton";
        _cancelButton.height = 20;

        _buttons = new HBox();
        _buttonsEnabled = false;
        setChanged(true);
        return _buttons;
    }

    // @Override from superclass
    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        updateDisplay(null);
        setChanged(false);
    }

    /**
     * Copies action data from the UI based on action type, and if the data is different,
     * creates a new FurniData with changes applied. Subclasses should override this function
     * to provide their own data updates.
     */
    protected function getUserModifications () :FurniData
    {
        // subclasses, do something here!
        return null;
    }

    /** Applies changes to the currently targetted object. */
    protected function applyChanges () :void
    {
        var newData :FurniData = getUserModifications();
        if (newData != null) {
            _controller.updateFurni(_furniData, newData);
        }
        setChanged(false);
    }

    /** Reverts changes by re-reading from the original furni data. */
    protected function revertChanges () :void
    {
        updateDisplay(_furniData);
    }

    /** Enables or disables the "apply" and "cancel" buttons, based on UI changes. */
    protected function setChanged (newValue :Boolean) :void
    {
        if (_applyButton == null || _cancelButton == null) {
            return; // not initialized yet!
        }

        if (newValue != _buttonsEnabled) {
            var fn :Function = newValue ? _buttons.addChild : _buttons.removeChild;
            fn(_applyButton);
            fn(_cancelButton);
            _buttonsEnabled = newValue;
        }
    }

    /** Event handler for widgets; enables the "apply" and "cancel" buttons. */
    protected function changedHandler (event :Event) :void
    {
        setChanged(true);
    }

    /** Event handler for widgets; saves updates, just like clicking the "apply" button. */
    protected function applyHandler (event :Event) :void
    {
        applyChanges();
    }

    protected var _furniData :FurniData;
    protected var _controller :RoomEditorController;
    protected var _buttons :HBox;
    protected var _buttonsEnabled :Boolean;
    protected var _applyButton :CommandButton;
    protected var _cancelButton :CommandButton;
}

}
