//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.events.Event;

import mx.binding.utils.BindingUtils;
import mx.containers.Grid;
import mx.controls.HSlider;
import mx.controls.TextInput;
import mx.events.FlexEvent;
import mx.events.SliderEvent;

import com.threerings.flex.GridUtil;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * Displays details about the room.
 */
public class RoomPanel extends BasePanel
{
    public function RoomPanel (controller :RoomEditorController)
    {
        super(controller);
    }

    // @Override from BasePanel
    override public function updateDisplay (data :FurniData) :void
    {
        super.updateDisplay(data);

        // ignore furni data - we don't care about which furni is selected,
        // only care about the room itself

        if (_controller.scene != null) {
            _name.text = _controller.scene.getName();
            this.enabled = true; // override base changes
        }
    }

    // @Override from superclass
    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(_name = new TextInput());
        _name.percentWidth = 100;

        addChild(makePanelButtons());
    }

    // @Override from superclass
    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        _name.addEventListener(Event.CHANGE, changedHandler);
        _name.addEventListener(FlexEvent.ENTER, applyHandler);
    }

    // @Override from BasePanel
    override protected function applyChanges () :void
    {
        super.applyChanges();

        if (_name.text != _controller.scene.getName()) {

            // configure an update
            var newscene :MsoyScene = _controller.scene.clone() as MsoyScene;
            var newmodel :MsoySceneModel = newscene.getSceneModel() as MsoySceneModel;
            newmodel.name = _name.text;
            _controller.updateScene(_controller.scene, newscene);
        }
    }

    protected var _name :TextInput;
}

}
