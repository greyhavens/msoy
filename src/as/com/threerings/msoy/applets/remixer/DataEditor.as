//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.TextEvent;

import mx.controls.CheckBox;
import mx.controls.ColorPicker;
import mx.controls.HSlider;
import mx.controls.Label;
import mx.controls.TextInput;

import mx.validators.NumberValidator;
import mx.validators.Validator;
import mx.validators.ValidationResult;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;

import com.whirled.remix.data.EditableDataPack;

public class DataEditor extends FieldEditor
{
    public function DataEditor (ctx :RemixContext, name :String)
    {
        var entry :Object = ctx.pack.getDataEntry(name);
        super(ctx, name, entry);

        _value = entry.value;

        addUsedCheckBox(entry);

        try {
            Object(this)["setup" + entry.type](entry, ctx);

        } catch (err :Error) {
            setupUnknown(entry);
        }

        // and specify whether the component is selected
        if (_component != null) {
            _component.enabled = _used.selected;
            _component.toolTip = entry.info;
        }
    }

    protected function setupBoolean (entry :Object, ctx :RemixContext) :void
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

    protected function setupString (
        entry :Object, ctx :RemixContext, validator :Validator = null) :void
    {
        var label :Label = new Label();
        updateLabel(label, entry);
        ctx.pack.addEventListener(EditableDataPack.DATA_CHANGED, function (event :ValueEvent) :void {
            if (event.value === entry.name) {
                updateLabel(label, ctx.pack.getDataEntry(entry.name));
            }
        });
        addComp(label);

        var dataEditor :DataEditor = this;
        var change :CommandButton = new CommandButton("View/Change", function () :void {
            new PopupEditor(dataEditor, ctx.pack.getDataEntry(entry.name), validator);
        });
        _component = change;
        addComp(change);
        addDescriptionLabel(entry);
    }

    protected function setupNumber (entry :Object, ctx :RemixContext) :void
    {
        var min :Number = Number(entry.min);
        var max :Number = Number(entry.max);

        // TODO: allow min/max either way, but allow a hint to specify to use the slider
        if (!isNaN(max) && !isNaN(min)) {
            var hslider :HSlider = new HSlider();
            hslider.minimum = min;
            hslider.maximum = max;
            hslider.value = Number(entry.value);
            hslider.addEventListener(Event.CHANGE, function (... ignored) :void {
                updateValue(hslider.value);
            });
            _component = hslider;
            addComp(hslider, 2);
            addDescriptionLabel(entry);

        } else {
            var val :NumberValidator = new NumberValidator();
            val.minValue = min;
            val.maxValue = max;
            setupString(entry, ctx, val);
        }
    }

    protected function setupColor (entry :Object, ctx :RemixContext) :void
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

    protected function updateLabel (label :Label, entry :Object) :void
    {
        label.text = (entry.value == null) ? "" : String(entry.value);
    }

    internal function updateValue (value :*) :void
    {
        if (value != _value) {
            _value = value;
            updateEntry();
        }
    }

    override protected function updateEntry () :void
    {
        _ctx.pack.setData(_name, _used.selected ? _value : null);
        setChanged();
    }

    protected var _value :*;
}
}
