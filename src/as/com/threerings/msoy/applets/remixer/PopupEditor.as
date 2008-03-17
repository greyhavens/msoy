//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.events.TextEvent;

import mx.controls.ButtonBar;
import mx.controls.Label;
import mx.controls.TextArea;
import mx.controls.TextInput;

import mx.containers.TitleWindow;
import mx.containers.Grid;

import mx.events.ValidationResultEvent;

import mx.managers.PopUpManager;

import mx.validators.Validator;
import mx.validators.ValidationResult;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;
import com.threerings.flex.PopUpUtil;

public class PopupEditor extends TitleWindow
{
    public function PopupEditor (parent :DataEditor, entry :Object, validator :Validator = null)
    {
        _parent = parent;
        _validator = validator;

        this.title = entry.name;
        var type :String = entry.type as String;

        var grid :Grid = new Grid();
        addChild(grid);
        GridUtil.addRow(grid, "Name:", entry.name as String);
        var desc :String = entry.info as String;
        if (desc == null) {
            desc = "<none>";
        }
        GridUtil.addRow(grid, "Description:", desc);
        GridUtil.addRow(grid, "Type:", type);

        if (type == "Number") {
            var min :Number = Number(entry.min);
            var max :Number = Number(entry.max);

            if (!isNaN(min)) {
                GridUtil.addRow(grid, "Minimum:", String(min));
            }
            if (!isNaN(max)) {
                GridUtil.addRow(grid, "Maximum:", String(max));
            }
        }

        if (_validator == null) {
            _txt = new TextArea();

        } else {
            _txt = new TextInput();
        }
        _txt.text = entry.value;

        GridUtil.addRow(grid, _txt, [2, 1]);

        var buttonBar :ButtonBar = new ButtonBar();
        _okBtn = new CommandButton("OK", close, true);
        buttonBar.addChild(_okBtn);
        buttonBar.addChild(new CommandButton("Cancel", close, false));
        GridUtil.addRow(grid, buttonBar, [2, 1]);

        if (_validator != null) {
            _validator.source = _txt;
            _validator.property = "text";
            _validator.addEventListener(ValidationResultEvent.VALID, checkValid);
            _validator.addEventListener(ValidationResultEvent.INVALID, checkValid);
            _validator.triggerEvent = Event.CHANGE; // TextEvent.TEXT_INPUT;
            _validator.trigger = IEventDispatcher(_txt);
        }

        PopUpManager.addPopUp(this, parent, true);
        PopUpUtil.center(this);
    }

    protected function checkValid (event :ValidationResultEvent) :void
    {
        _okBtn.enabled = (event.type == ValidationResultEvent.VALID);
    }

    protected function close (save :Boolean) :void
    {
        if (save) {
            _parent.updateValue(_txt.text);
        }

        PopUpManager.removePopUp(this);
    }

    protected var _parent :DataEditor;

    protected var _validator :Validator;

    protected var _txt :Object; // either a TextInput or TextArea (no common text base class)

    protected var _okBtn :CommandButton;
}
}

