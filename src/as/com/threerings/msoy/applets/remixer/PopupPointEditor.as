//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.IEventDispatcher;

import flash.geom.Point;

import mx.controls.TextInput;

import mx.containers.Grid;

import mx.events.ValidationResultEvent;

import mx.validators.NumberValidator;

import com.threerings.flex.GridUtil;

public class PopupPointEditor extends PopupEditor
{
    override protected function configureUI (ctx :RemixContext, entry :Object, grid :Grid) :void
    {
        _x = new TextInput();
        _y = new TextInput();

        GridUtil.addRow(grid, ctx.REMIX.get("l.x"), _x);
        GridUtil.addRow(grid, ctx.REMIX.get("l.y"), _y);

        configureValidator(_x, checkXValid);
        configureValidator(_y, checkYValid);

        var p :Point = entry.value as Point;
        if (p != null) {
            _x.text = String(p.x);
            _y.text = String(p.y);
        }

        kick(_x, _y);
    }

    protected function configureValidator (
        ti :TextInput, validateFn :Function, minValue :Number = NaN) :void
    {
        var validator :NumberValidator = new NumberValidator();
        validator.minValue = minValue;
        validator.source = ti;
        validator.property = "text";
        validator.addEventListener(ValidationResultEvent.VALID, validateFn);
        validator.addEventListener(ValidationResultEvent.INVALID, validateFn);
        validator.triggerEvent = Event.CHANGE;
        validator.trigger = ti;
        // we don't need to retain a reference, setting the trigger adds the validator
        // as a listener to the trigger
    }

    protected function kick (... args) :void
    {
        for each (var ed :IEventDispatcher in args) {
            ed.dispatchEvent(new Event(Event.CHANGE));
        }
    }

    override protected function getNewValue () :Object
    {
        return new Point(Number(_x.text), Number(_y.text));
    }

    protected function checkXValid (event :ValidationResultEvent) :void
    {
        _xValid = (event.type == ValidationResultEvent.VALID);
        checkValid();
    }

    protected function checkYValid (event :ValidationResultEvent) :void
    {
        _yValid = (event.type == ValidationResultEvent.VALID);
        checkValid();
    }

    protected function checkValid () :void
    {
        _okBtn.enabled = _xValid && _yValid;
    }

    protected var _x :TextInput;
    protected var _y :TextInput;

    protected var _xValid :Boolean;
    protected var _yValid :Boolean;
}
}

