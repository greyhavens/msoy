//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.events.TextEvent;

import mx.controls.Label;
import mx.controls.TextArea;
import mx.controls.TextInput;

import mx.containers.Grid;
import mx.containers.HBox;
import mx.containers.TitleWindow;

import mx.events.ValidationResultEvent;

import mx.managers.PopUpManager;

import mx.validators.Validator;
import mx.validators.ValidationResult;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;
import com.threerings.flex.PopUpUtil;

public class PopupEditor extends TitleWindow
{
    public function PopupEditor (
        ctx :RemixContext, parent :DataEditor, entry :Object, validator :Validator = null)
    {
        _parent = parent;
        _validator = validator;

        this.title = entry.name;
        var type :String = entry.type as String;

        var grid :Grid = new Grid();
        addChild(grid);
        GridUtil.addRow(grid, ctx.REMIX.get("l.name"), entry.name as String);
        var desc :String = entry.info as String;
        if (desc == null) {
            desc = ctx.REMIX.get("m.none");
        }
        GridUtil.addRow(grid, ctx.REMIX.get("l.desc"), desc);
        GridUtil.addRow(grid, ctx.REMIX.get("l.type"), type);

        if (type == "Number") {
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
        }
        _txt.text = entry.value;

        GridUtil.addRow(grid, _txt, [2, 1]);

        var buttonBar :HBox = new HBox();
        buttonBar.setStyle("horizontalAlign", "right");
        buttonBar.percentWidth = 100;
        buttonBar.addChild(new CommandButton(ctx.REMIX.get("b.cancel"), close, false));
        _okBtn = new CommandButton(ctx.REMIX.get("b.ok"), close, true);
        buttonBar.addChild(_okBtn);
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

