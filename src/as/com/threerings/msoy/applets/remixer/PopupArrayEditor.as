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

// we could almost just omit this and use the String editor, but the little element count is groovy
public class PopupArrayEditor extends PopupStringEditor
{
    override protected function configureUI (ctx :RemixContext, entry :Object, grid :Grid) :void
    {
        _elemCount = new Label();

        super.configureUI(ctx, entry, grid);

        GridUtil.addRow(grid, ctx.REMIX.get("l.elemCount"), _elemCount);
    }

    override protected function handleTextValidation (event :Event) :void
    {
        super.handleTextValidation(event);
        if (_okBtn.enabled) {
            _elemCount.text = String((getNewValue() as Array).length);
        }
    }

    protected var _elemCount :Label;
}
}
