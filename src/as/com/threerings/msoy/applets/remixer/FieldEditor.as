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

    protected function createDescriptionLabel (entry :Object) :Label
    {
        var lbl :Label = new Label();
        var tip :String = entry.info;
        if (tip != null) {
            lbl.text = "[ " + tip + " ]";
        }
        return lbl;
    }

    protected function addPresentBox (entry :Object) :void
    {
        _present = new CheckBox();
        _present.selected = (entry.value != null);
        _present.enabled = entry.optional;
        _present.addEventListener(Event.CHANGE, handlePresentToggled);
        addComp(_present);
    }

    protected function addComp (comp :UIComponent, colSpan :int = 1) :void
    {
        var item :GridItem = GridUtil.addToRow(this, comp);
        item.colSpan = colSpan;
    }

    protected function handlePresentToggled (event :Event) :void
    {
        _component.enabled = _present.selected;
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

    protected var _present :CheckBox;

    protected var _pack :EditableDataPack;
}
}
