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

        var entry :Object = pack.getFileEntry(name);

        addPresentBox(entry);

        var lbl :Label = new Label();
        lbl.text = entry.value as String;
        addComp(lbl, 1);

        var change :CommandButton = CommandButton.create("View/Change (TODO)", function () :void {
            // TODO
        });
        addComp(change, 1);
        _component = change;
    }

    // Necessary?
    protected function setupUnknown (entry :Object) :void
    {
        var lbl :Label = new Label();
        lbl.text = "Unknown entry of type '" + entry.type + "'.";

        addComp(lbl, 2);
    }

    override protected function updateEntry () :void
    {
        // TODO
        if (!_present.selected) {
            _pack.replaceFile(_name, null, null);
            setChanged();
        }
    }
}
}
