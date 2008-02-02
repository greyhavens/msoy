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

    public function FieldEditor (pack :EditableDataPack, name :String)
    {
        _pack = pack;
        _name = name;

        var lbl :Label = new Label();
        lbl.text = name;
        addComp(lbl);
    }

    protected function addPresentBox (entry :Object) :void
    {
        // TODO: bind all this shit up?
        var presentBox :CheckBox = new CheckBox();
        presentBox.selected = (entry.value != null);
        presentBox.enabled = entry.optional;
        // TODO: modify presentBox..
        addComp(presentBox);
    }

    protected function addComp (comp :UIComponent, colSpan :int = 1) :void
    {
        var item :GridItem = GridUtil.addToRow(this, comp);
        item.colSpan = colSpan;
    }

    /**
     * May be called or set as an event handler.
     */
    protected function setChanged (... ignored) :void
    {
        dispatchEvent(new Event(FIELD_CHANGED, true));
    }

    /** The name of the field we're editing. */
    protected var _name :String;

    protected var _pack :EditableDataPack;
}
}
