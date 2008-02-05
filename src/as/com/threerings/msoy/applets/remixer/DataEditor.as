//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.TextEvent;

import mx.controls.CheckBox;
import mx.controls.ColorPicker;
import mx.controls.Label;
import mx.controls.TextInput;

import mx.validators.NumberValidator;
import mx.validators.Validator;
import mx.validators.ValidationResult;

import com.threerings.flex.CommandButton;

import com.whirled.remix.data.EditableDataPack;

public class DataEditor extends FieldEditor
{
    public function DataEditor (pack :EditableDataPack, name :String)
    {
        var entry :Object = pack.getDataEntry(name);
        super(pack, name, entry);

        _value = entry.value;

        addUsedCheckBox(entry);

        try {
            Object(this)["setup" + entry.type](entry);

        } catch (err :Error) {
            setupUnknown(entry);
        }

        // and specify whether the component is selected
        if (_component != null) {
            _component.enabled = _used.selected;
            _component.toolTip = entry.info;
        }
    }

    protected function setupBoolean (entry :Object) :void
    {
        var tog :CheckBox = new CheckBox();
        tog.selected = Boolean(entry.value);
        tog.addEventListener(Event.CHANGE, function (... ignored) :void {
            updateValue(tog.selected);
        });
        _component = tog;

        addComp(tog, 2);
        addDescriptionLabel(entry);
    }

    protected function setupString (entry :Object, validator :Validator = null) :void
    {
        var display :Label = new Label();
        if (entry.value != null) {
            display.text = String(entry.value);
        }
        addComp(display);

        var dataEditor :DataEditor = this;
        var change :CommandButton = CommandButton.create("View/Change", function () :void {
            new PopupEditor(dataEditor, entry, display, validator);
        });
        _component = change;
        addComp(change);
        addDescriptionLabel(entry);
    }

    protected function setupNumber (entry :Object) :void
    {
        setupString(entry, new NumberValidator());
    }

    protected function setupColor (entry :Object) :void
    {
        var picker :ColorPicker = new ColorPicker();
        picker.selectedColor = uint(entry.value);
        _value = picker.selectedColor;
        picker.addEventListener(Event.CLOSE, function (... ignored) :void {
            updateValue(picker.selectedColor);
        });
        _component = picker;

        addComp(picker, 2);
        addDescriptionLabel(entry);
    }

    protected function setupUnknown (entry :Object) :void
    {
        var lbl :Label = new Label();
        lbl.text = "Unknown entry of type '" + entry.type + "'.";

        addComp(lbl, 3);
    }

    internal function updateValue (value :*) :void
    {
        _value = value;
        updateEntry();
    }

    override protected function updateEntry () :void
    {
        _pack.setData(_name, _used.selected ? _value : null);
        setChanged();
    }

    protected var _value :*;
}
}
