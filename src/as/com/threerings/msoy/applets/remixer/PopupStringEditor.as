//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.events.TextEvent;

import mx.controls.TextArea;
import mx.controls.TextInput;

import mx.containers.Grid;
import mx.containers.HBox;

import mx.events.FlexEvent;
import mx.events.ValidationResultEvent;

import mx.validators.Validator;
import mx.validators.ValidationResult;

import com.threerings.flex.GridUtil;

public class PopupStringEditor extends PopupEditor
{
    public function PopupStringEditor (validator :Validator = null) :void
    {
        _validator = validator;
        super();
    }

    override protected function configureUI (ctx :RemixContext, entry :Object, grid :Grid) :void
    {
        if (entry.type == "Number") {
            var min :Number = Number(entry.min);
            var max :Number = Number(entry.max);

            if (!isNaN(min)) {
                GridUtil.addRow(grid, ctx.REMIX.get("l.min"), String(min));
            }
            if (!isNaN(max)) {
                GridUtil.addRow(grid, ctx.REMIX.get("l.max"), String(max));
            }
        }

        if (_validator == null) {
            _txt = new TextArea();

        } else {
            _txt = new TextInput();
            TextInput(_txt).addEventListener(FlexEvent.ENTER, handleEnterPressed);
        }
        GridUtil.addRow(grid, _txt, [2, 1]);

        if (_validator != null) {
            _validator.source = _txt;
            _validator.property = "text";
            _validator.addEventListener(ValidationResultEvent.VALID, checkValid);
            _validator.addEventListener(ValidationResultEvent.INVALID, checkValid);
            _validator.triggerEvent = Event.CHANGE; // TextEvent.TEXT_INPUT;
            _validator.trigger = IEventDispatcher(_txt);
        }

        _txt.text = entry.value;
    }

    override protected function getNewValue () :Object
    {
        return _txt.text;
    }

    /**
     * Submit the change, as long as things are valid.
     */
    protected function handleEnterPressed (event :FlexEvent) :void
    {
        if (_okBtn.enabled) {
            close(true);
        }
    }

    protected function checkValid (event :ValidationResultEvent) :void
    {
        _okBtn.enabled = (event.type == ValidationResultEvent.VALID);
    }

    protected var _validator :Validator;

    protected var _txt :Object; // either a TextInput or TextArea (no common text base class)
}
}

