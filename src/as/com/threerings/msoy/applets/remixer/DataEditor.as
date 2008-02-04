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
        super(pack, name);

        var entry :Object = pack.getDataEntry(name);
        _value = entry.value;

        addPresentBox(entry);

        try {
            Object(this)["setup" + entry.type](entry);

        } catch (err :Error) {
            setupUnknown(entry);
        }

        // and specify whether the component is selected
        if (_component != null) {
            _component.enabled = _present.selected;
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
    }

    protected function setupString (entry :Object) :void
    {
        var input :TextInput = new TextInput();
        if (entry.value != null) {
            input.text = String(entry.value);
        }
//        input.addEventListener(TextEvent.TEXT_INPUT, function (... ignored) :void {
//            updateValue(input.text);
//        });
        input.addEventListener(Event.CHANGE, function (... ignored) :void {
            updateValue(input.text);
        });
        _component = input;

        addComp(input, 2);
    }

    protected function setupNumber (entry :Object) :void
    {
        setupString(entry);
        var val :NumberValidator = new NumberValidator();
        val.source = _component;
        val.property = "text";
        _validator = val;
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
    }

    protected function setupUnknown (entry :Object) :void
    {
        var lbl :Label = new Label();
        lbl.text = "Unknown entry of type '" + entry.type + "'.";

        addComp(lbl, 2);
    }

    protected function updateValue (value :*) :void
    {
        _value = value;
        updateEntry();
    }

    override protected function updateEntry () :void
    {
        if (_validator != null) {
            var results :Array = _validator.validate().results;
            for each (var result :ValidationResult in results) {
                if (result.isError) {
                    // do not update...
                    return;
                }
            }
        }

        _pack.setData(_name, _present.selected ? _value : null);
        setChanged();
    }

    protected var _value :*;

    protected var _validator :Validator;
}
}
