//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.events.TextEvent;

import mx.controls.Label;

import mx.containers.Grid;
import mx.containers.HBox;

import mx.events.FlexEvent;
import mx.events.ValidationResultEvent;

import mx.validators.Validator;
import mx.validators.ValidationResult;

import com.threerings.flex.GridUtil;

public class PopupArrayEditor extends PopupStringEditor
{
    override protected function configureUI (ctx :RemixContext, entry :Object, grid :Grid) :void
    {
        super.configureUI(ctx, entry, grid);
        if (entry.value == null) {
            _txt.text = "";
        } else {
            // TODO: better parse?
            _txt.text = (entry.value as Array).join();
        }

        _elemCount = new Label();
        GridUtil.addRow(grid, ctx.REMIX.get("l.elemCount"), _elemCount);

        // now listen for _txt changes and update the elemCount
        _txt.addEventListener(Event.CHANGE, handleTextChange);
        handleTextChange(null)
    }

    protected function handleTextChange (event :Event) :void
    {
        _elemCount.text = String((getNewValue() as Array).length);
    }

    override protected function getNewValue () :Object
    {
        return String(_txt.text).split(",");
    }

    protected var _elemCount :Label;
}
}
