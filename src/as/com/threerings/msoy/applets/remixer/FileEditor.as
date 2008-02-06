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
        var entry :Object = pack.getFileEntry(name);
        super(pack, name, entry);

        addUsedCheckBox(entry);

        var lbl :Label = new Label();
        lbl.text = entry.value as String;
        addComp(lbl);

        var change :CommandButton = new CommandButton("View/Change", function () :void {
            // TODO
        });
        _component = change;
        addComp(change);
        addDescriptionLabel(entry);
    }

    // Necessary?
    protected function setupUnknown (entry :Object) :void
    {
        var lbl :Label = new Label();
        lbl.text = "Unknown entry of type '" + entry.type + "'.";

        addComp(lbl, 3);
    }

    override protected function updateEntry () :void
    {
        // TODO
        if (!_used.selected) {
            _pack.replaceFile(_name, null, null);
            setChanged();
        }
    }
}
}
