//
// $Id$

package com.threerings.msoy.world.client.editor {

import mx.containers.VBox;
import mx.controls.Button;
import mx.controls.Label;
import mx.controls.Spacer;

import com.threerings.msoy.world.data.FurniData;

/**
 * Displays details about the position, size, and other numeric values of the furni.
 */
public class DetailsPanel extends VBox
{
    public function DetailsPanel (controller :RoomEditorController)
    {
        _controller = controller;
    }

    /** Clears the UI. */
    public function clearDisplay () :void
    {
    }
    
    /** Updates the UI from given data. */
    public function updateDisplay (data :FurniData) :void
    {
    }

        // @Override from VBox
    override protected function createChildren () :void
    {
        var b :Button = new Button();
        addChild(b);
        addChild(new Button());
    }
    

    protected var _controller :RoomEditorController;
}

}
