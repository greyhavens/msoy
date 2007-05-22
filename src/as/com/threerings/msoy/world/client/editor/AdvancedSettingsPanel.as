//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.events.KeyboardEvent;
import flash.ui.Keyboard;

import mx.binding.utils.BindingUtils;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.ViewStack;
import mx.controls.Button;
import mx.controls.CheckBox;
import mx.controls.Label;
import mx.controls.Spacer;
import mx.controls.TextInput;
import mx.core.Container;

import com.threerings.flash.Vector3;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.client.RoomMetrics;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * "It's not stupid, it's advanced!" -- Invader Zim
 *
 * Advanced settings for the different room editing tools, displayed as a sub-panel
 * of the room editor.
 */
public class AdvancedSettingsPanel extends ViewStack
{
    public static const MOVE_CONSTRAINT_NONE :int = 0;   
    public static const MOVE_CONSTRAINT_X_AXIS :int = 1;
    public static const MOVE_CONSTRAINT_Y_AXIS :int = 2;
    public static const MOVE_CONSTRAINT_Z_AXIS :int = 3;

    public static const MOVE_CONSTRAINT_NORMALS :Object = {
        0: null,
        1: RoomMetrics.N_RIGHT,
        2: RoomMetrics.N_UP,
        3: RoomMetrics.N_AWAY
    };
    
    public function AdvancedSettingsPanel (ctrl :RoomEditController)
    {
        _ctrl = ctrl;
    }
    
    /** Returns the current movement constraint, as one of the MOVE_CONSTRAINT_* constants. */
    public function getMoveConstraint () :int
    {
        for each (var def :Object in MOVE_DEFS) {
                if ((_moveCheckboxes[def.label] as Button).selected) {
                    return int(def.constraint);
                }
            }
        return MOVE_CONSTRAINT_NONE;
    }

    /** Returns the current movement constraint as one of the RoomMetrics.N_* normal constants,
     *  suitable for passing to RoomLayout.pointTo{Furni|Avatar}Location(). */
    public function getMoveConstraintAsNormal () :Vector3
    {
        return MOVE_CONSTRAINT_NORMALS[getMoveConstraint()];
    }        

    /** Returns true if scaling should be proportional (i.e. retaining existing aspect ratio). */
    public function get proportionalScaling () :Boolean
    {
        return ! _freescale.selected;
    }
    
    /** Toggles whether this panel is visible and included in layout. */
    public function toggleVisibility () :void
    {
        this.visible = this.includeInLayout = (! this.visible);
    }

    /** Processes key input from the controller, and adjusts advanced settings accordingly. */
    public function handleKeyboard (action :String, eventType :String, keyCode :uint) :void
    {
        var updateFromKeyboard :Function = function (checkbox :CheckBox) :void {
            switch (eventType) {
            case KeyboardEvent.KEY_DOWN: checkbox.selected = true;  break;
            case KeyboardEvent.KEY_UP:   checkbox.selected = false; break;
            }
        }

        switch (action) {
        case RoomEditController.ACTION_MOVE:
            // find the check box
            var def :Object = findDefinition(MOVE_DEFS, "key", keyCode);
            if (def != null) {
                var checkbox :CheckBox = _moveCheckboxes[def.label];
                if (checkbox != null) {
                    updateFromKeyboard(checkbox);
                }
            }
            break;

        case RoomEditController.ACTION_SCALE:
            // just set the checkbox appropriately
            if (keyCode == Keyboard.SHIFT) {
                updateFromKeyboard(_freescale);
            }
            break;
        }
    }

    /**
     * Called by the room editor controller when a new action type is selected.
     * Causes a new properties panel to be selected and displayed.
     */
    public function handleActionSelection (action :String) :void
    {
        var panel :Container = _panels[action] as Container;
        this.selectedChild = (panel != null) ? panel : _panels[RoomEditController.ACTION_NONE];
    }
        
    /** Called by the room editor controller, to update displayed furni position. */
    public function updatePosition (loc :MsoyLocation) :void
    {
        for (var key :String in _positionFields) {
            (_positionFields[key] as TextInput).text = formatNumber(loc[key] as Number);
        }
    }

    /** Called by the room editor controller, to update displayed furni scale. */
    public function updateScale (x :Number, y :Number) :void {
        (_scaleFields.x as TextInput).text = formatNumber(x);
        (_scaleFields.y as TextInput).text = formatNumber(y);
    }

    /** Formats a number for display in the tiny text fields. */
    public function formatNumber (n :Number) :String
    {
        // hack hack hack to format a number to no more than two decimal places
        return String(Math.round(100 * n) / 100);
    }
                

    // from ViewStack
    override protected function createChildren () :void
    {
        super.createChildren();

        var addLabel :Function = function (parent :Container, text :String) :void {
            var label :Label = new Label();
            label.text = text;
            parent.addChild(label);
        }
        
        this.styleName = "roomEditAdvancedContainer";
        this.resizeToContent = true;
        toggleVisibility(); // start invisible

        // make each panel for the stack
        _panels = new Object();

        // empty panel (maybe some info text here?)
        var defaultPanel :HBox = new HBox();
        addChild(defaultPanel);
        _panels[RoomEditController.ACTION_NONE] = defaultPanel;

        addLabel(defaultPanel, Msgs.EDITING.get("l.advanced_none"));
        
        // move settings
        var moves :HBox = new HBox();
        moves.styleName = "roomEditAdvancedContainer";
        addChild(moves);
        _panels[RoomEditController.ACTION_MOVE] = moves;

        var box :Container = new VBox();
        addLabel(box, Msgs.EDITING.get("l.axis"));
        addLabel(box, Msgs.EDITING.get("l.location"));
        moves.addChild(box);

        _moveCheckboxes = new Object();
        _positionFields = new Object();

        for each (var def :Object in MOVE_DEFS) {
            box = new VBox();
            box.styleName = "roomEditAdvancedContainer";
            moves.addChild(box);
            
            // axis checkbox
            var axis :CheckBox = new CheckBox();
            axis.label = def.label as String;
            box.addChild(axis);

            // position number
            var number :TextInput = new TextInput();
            number.text = "0.0";
            number.width = 35;
            number.editable = number.enabled = false;
            box.addChild(number);

            // store references
            _moveCheckboxes[def.label] = axis;
            _positionFields[def.loc] = number;

            // checkbox value binding
            BindingUtils.bindSetter(makeClearingFunction(axis), axis, "selected");            
        }

        // scale settings
        var scales :HBox = new HBox();
        scales.styleName = "roomEditAdvancedContainer";
        addChild(scales);
        _panels[RoomEditController.ACTION_SCALE] = scales;

        box = new VBox();
        addLabel(box, Msgs.EDITING.get("l.axis"));
        addLabel(box, Msgs.EDITING.get("l.scale"));
        scales.addChild(box);

        _scaleFields = new Object();

        for each (def in SCALE_DEFS) {
            box = new VBox();
            box.styleName = "roomEditAdvancedContainer";
            scales.addChild(box);
            
            // scale label
            addLabel(box, def.label as String);

            // scale number
            var scale :TextInput = new TextInput();
            scale.text = "0.0";
            scale.width = 35;
            scale.editable = scale.enabled = false;
            box.addChild(scale);

            // store references
            _scaleFields[def.key] = scale;
        }
        
        box = new VBox();
        _freescale = new CheckBox();
        _freescale.label = Msgs.EDITING.get("l.freescale");
        box.addChild(_freescale);
        
        scales.addChild(box);

    }

    override protected function childrenCreated () :void
    {
        super.childrenCreated();
        
        // finally, let's not show one until the player selects an editing mode
        handleActionSelection(null);
    }

    /** Helper function, makes a handler function that will clear all other checkboxes
     *  when one of them is selected. */
    protected function makeClearingFunction (checkbox :CheckBox) :Function
    {
        // poor man's currying. :)
        return function (selected :Boolean) :void {
            if (selected) {
                for (var key :String in _moveCheckboxes) {
                    var candidate :CheckBox = _moveCheckboxes[key] as CheckBox;
                    if (candidate != null && candidate != checkbox) {
                        candidate.selected = false;
                    }
                }
            }
        }
    }

    /** Retrieval accessor for arrays of definition objects. */
    protected function findDefinition (defs :Array, key :Object, test :*) :Object
    {
        for each (var def :Object in defs) {
                if (def[key] == test) {
                    return def;
                }
            }
        return null;
    }
    
    protected static const MOVE_DEFS :Array = [
        { label: "X", loc: "x", constraint: MOVE_CONSTRAINT_X_AXIS },
        { label: "Y", loc: "y", constraint: MOVE_CONSTRAINT_Y_AXIS, key: Keyboard.SHIFT },
        { label: "Z", loc: "z", constraint: MOVE_CONSTRAINT_Z_AXIS, key: Keyboard.CONTROL }
        ];

    protected static const SCALE_DEFS :Array = [
        { label: "X", key: "x" },
        { label: "Y", key: "y" }
        ];

    protected var _ctrl :RoomEditController;

    /** Associative list that maps from some of the ACTION_* constants to their appropriate
     *  advanced option panel. Not all actions have a panel; they should use _panels[ACTION_NONE]
     *  as a default value. */
    protected var _panels :Object;
    
    /** Associative list that maps from axis labels specified in MOVE_DEFS to the appropriate
     *  CheckBox widgets, with the same label (e.g. "X" -> CheckBox). */
    protected var _moveCheckboxes :Object;

    /** Checkbox that specifies whether object scaling should be proportional or free. */
    protected var _freescale :CheckBox;

    /** Associative list that maps from an axis name to the appropriate TextInput widget
     *  (e.g. "x" -> TextInput). Axis names are the same as their corresponding
     *  attribute names on MsoyLocation class. */
    protected var _positionFields :Object;

    /** Associative list that maps from the key value in SCALE_DEFS to the appropriate
     *  TextInput widget (e.g. "x" -> TextInput). */
    protected var _scaleFields :Object;

}
}
    
