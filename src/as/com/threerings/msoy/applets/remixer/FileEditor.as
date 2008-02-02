//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;

import mx.controls.CheckBox;
import mx.controls.Label;

import com.threerings.flex.CommandButton;

import com.whirled.remix.data.EditableDataPack;

public class FileEditor extends FieldEditor
{
    public function FileEditor (pack :EditableDataPack, name :String)
    {
        super(pack, name);

        addPresentBox({ optional: true });

        var lbl :Label = new Label();
        lbl.text = "coming soon";
        addComp(lbl, 2);
    }

    protected function setupUnknown (entry :Object) :void
    {
        var lbl :Label = new Label();
        lbl.text = "Unknown entry of type '" + entry.type + "'.";

        addComp(lbl, 2);
    }
}
}
