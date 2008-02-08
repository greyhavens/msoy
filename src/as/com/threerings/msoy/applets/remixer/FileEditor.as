//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.utils.ByteArray;

import mx.controls.Label;

import com.threerings.flex.CommandButton;

import com.whirled.remix.data.EditableDataPack;

public class FileEditor extends FieldEditor
{
    public function FileEditor (pack :EditableDataPack, name :String, serverURL :String)
    {
        var entry :Object = pack.getFileEntry(name);
        super(pack, name, entry);
        _serverURL = serverURL;

        addUsedCheckBox(entry);

        _label = new Label();
        _label.text = entry.value as String;
        addComp(_label);

        // TODO, this'll change
        var change :CommandButton = new CommandButton("View/Change", showFile);
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

    internal function updateValue (filename :String, bytes :ByteArray) :void
    {
        _label.text = filename;
        _bytes = bytes;
        updateEntry();

        _pack.replaceFile(_name, filename, bytes);
        setChanged();
    }

    override protected function updateEntry () :void
    {
        _pack.replaceFile(_name, _used.selected ? _label.text : null, _bytes);
        setChanged();
    }

    protected function showFile () :void
    {
        new PopupFilePreview(this, _name, _pack, _serverURL);
    }

    protected var _label :Label;

    protected var _bytes :ByteArray;

    protected var _serverURL :String;
}
}
