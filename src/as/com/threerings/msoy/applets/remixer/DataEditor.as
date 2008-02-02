//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;

import mx.controls.CheckBox;
import mx.controls.ColorPicker;
import mx.controls.Label;

import com.threerings.flex.CommandButton;

import com.whirled.remix.data.EditableDataPack;

public class DataEditor extends FieldEditor
{
    public function DataEditor (pack :EditableDataPack, name :String)
    {
        super(pack, name);

        var entry :Object = pack.getDataEntry(name);

        addPresentBox(entry);

        switch (String(entry.type)) {
        case "Color":
            setupColor(entry);
            break;

//        case "String":
//            setupString(entry);
//            break;

        default:
            setupUnknown(entry);
            break;
        }
    }

    protected function setupColor (entry :Object) :void
    {
        var picker :ColorPicker = new ColorPicker();
        picker.selectedColor = uint(entry.value);
        picker.addEventListener(Event.CLOSE, function (... ignored) :void {
            _pack.setData(_name, picker.selectedColor);
            setChanged();
        });

        addComp(picker, 2);
    }

    protected function setupUnknown (entry :Object) :void
    {
        var lbl :Label = new Label();
        lbl.text = "Unknown entry of type '" + entry.type + "'.";

        addComp(lbl, 2);
    }
}
}
