//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.events.TextEvent;

import mx.controls.TextArea;
import mx.controls.TextInput;

import mx.containers.Grid;

import mx.events.FlexEvent;
import mx.events.ValidationResultEvent;

import mx.validators.Validator;

import com.threerings.flex.GridUtil;

/**
 * Can be used to edit almost any type.
 */
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

        var textToShow :String;
        if (entry.type == "String" || Boolean(entry.useArea)) {
            _txt = new TextArea();
            _txt.width = 400;
            _txt.height = 400;
            textToShow = entry.value; // allow "s and so forth

        } else {
            _txt = new TextInput();
            IEventDispatcher(_txt).addEventListener(FlexEvent.ENTER, handleEnterPressed);
            textToShow = entry.toString(); // set the text formatted
        }
        if (_validator != null) {
            _validator.source = _txt;
            _validator.property = "text";
            _validator.addEventListener(ValidationResultEvent.VALID, checkValid);
            _validator.addEventListener(ValidationResultEvent.INVALID, checkValid);
            _validator.triggerEvent = Event.CHANGE; // TextEvent.TEXT_INPUT;
            _validator.trigger = IEventDispatcher(_txt);

        } else {
            IEventDispatcher(_txt).addEventListener(Event.CHANGE, handleTextValidation);
        }
        _txt.text = textToShow;
        handleTextValidation(null); // validate now
        GridUtil.addRow(grid, _txt, [2, 1]);
    }

    override protected function getNewValue () :Object
    {
        return _entry.fromString(_txt.text);
    }

    protected function handleTextValidation (event :Event) :void
    {
        try {
            getNewValue();
            _okBtn.enabled = true;
        } catch (e :Error) {
            trace("Invalid value: " + e);
            _okBtn.enabled = false;
        }
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
