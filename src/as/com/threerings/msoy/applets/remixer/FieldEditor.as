//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;

import mx.containers.GridItem;
import mx.containers.GridRow;

import mx.controls.CheckBox;
import mx.controls.Label;

import mx.core.UIComponent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;

import com.whirled.remix.data.EditableDataPack;

public class FieldEditor extends GridRow
{
    /**
     * An event dispatched upwards when a field is edited.
     */
    public static const FIELD_CHANGED :String = "remixFieldChanged";

    public function FieldEditor (pack :EditableDataPack, name :String, entry :Object)
    {
        _pack = pack;
        _name = name;

        var lbl :Label = new Label();
        lbl.toolTip = entry.info;
        lbl.text = name;
        addComp(lbl);
    }

    protected function addDescriptionLabel (entry :Object) :void
    {
        var lbl :Label = new Label();
        var tip :String = entry.info;
        if (tip != null) {
            lbl.text = "[ " + tip + " ]";
        }
        addComp(lbl);
    }

    protected function addUsedCheckBox (entry :Object) :void
    {
        _used = new CheckBox();
        _used.selected = (entry.value != null);
        _used.enabled = entry.optional;
        _used.addEventListener(Event.CHANGE, handleUsedToggled);
        addComp(_used);
    }

    protected function addComp (comp :UIComponent, colSpan :int = 1) :void
    {
        var item :GridItem = GridUtil.addToRow(this, comp);
        item.colSpan = colSpan;
    }

    protected function handleUsedToggled (event :Event) :void
    {
        _component.enabled = _used.selected;
        updateEntry();
    }

    protected function updateEntry () :void
    {
        throw new Error("abstract");
    }

    /**
     * May be called or set as an event handler.
     */
    protected function setChanged (... ignored) :void
    {
        // dispatch it upwards
        dispatchEvent(new Event(FIELD_CHANGED, true));
    }

    /** The name of the field we're editing. */
    protected var _name :String;

    /** The component that's doing the editing. It should be able to
     * enabled/disabled. */
    protected var _component :UIComponent;

    protected var _used :CheckBox;

    protected var _pack :EditableDataPack;
}
}
