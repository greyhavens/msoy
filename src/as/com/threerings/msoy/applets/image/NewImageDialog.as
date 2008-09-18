//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.BitmapData;

import flash.events.Event;

import mx.controls.ButtonBar;
import mx.controls.ColorPicker;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;
import mx.controls.TextInput;

import mx.containers.Grid;
import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.events.ValidationResultEvent;

import mx.managers.PopUpManager;

import mx.validators.NumberValidator;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;
import com.threerings.flex.PopUpUtil;

public class NewImageDialog extends TitleWindow
{
    public function NewImageDialog (ctx :ImageContext, sizeRestriction :SizeRestriction)
    {
        title = ctx.IMAGE.get("t.new_image");

        var box :VBox = new VBox();
        box.setStyle("horizontalAlign", "right");
        addChild(box);

        _width = new TextInput();
        _height = new TextInput();
        _width.restrict = "0-9";
        _height.restrict = "0-9";
        _width.maxChars = 4;
        _height.maxChars = 4;
        _width.maxWidth = 100;
        _height.maxWidth = 100;

        if (sizeRestriction.forced != null) {
            _width.text = String(sizeRestriction.forced.x);
            _height.text = String(sizeRestriction.forced.y);
            _width.enabled = false;
            _height.enabled = false;

        } else {
            var widthVal :NumberValidator = new NumberValidator();
            widthVal.minValue = 1;
            widthVal.maxValue = sizeRestriction.maxWidth;
            widthVal.source = _width;
            widthVal.property = "text";
            widthVal.addEventListener(ValidationResultEvent.VALID, checkValid);
            widthVal.addEventListener(ValidationResultEvent.INVALID, checkValid);
            widthVal.triggerEvent = Event.CHANGE;
            widthVal.trigger = _width;
            _widthValidator = widthVal;

            var heightVal :NumberValidator = new NumberValidator();
            heightVal.minValue = 1;
            heightVal.maxValue = sizeRestriction.maxHeight;
            heightVal.source = _height;
            heightVal.property = "text";
            heightVal.addEventListener(ValidationResultEvent.VALID, checkValid);
            heightVal.addEventListener(ValidationResultEvent.INVALID, checkValid);
            heightVal.triggerEvent = Event.CHANGE;
            heightVal.trigger = _height;
            _heightValidator = heightVal;

            _width.text = String(isNaN(sizeRestriction.maxWidth) ? 200 : sizeRestriction.maxWidth);
            _height.text =
                String(isNaN(sizeRestriction.maxHeight) ? 200 : sizeRestriction.maxHeight);
        }

        _fillColor = new ColorPicker();
        _fillColor.selectedColor = 0xFFFFFF;
        var trans :RadioButton = new RadioButton();
        trans.label = ctx.IMAGE.get("l.transparent");
        trans.value = false;
        trans.selected = true;
        trans.group = _fillGroup;

        var fill :RadioButton = new RadioButton();
        fill.label = ctx.IMAGE.get("l.fill");
        fill.value = true;
        fill.group = _fillGroup;

        // kinda pointless to use Grids here...
        var grid :Grid = new Grid();
        GridUtil.addRow(grid, ctx.IMAGE.get("l.width"), _width, ctx.IMAGE.get("l.height"), _height);
        box.addChild(grid);

        grid = new Grid();
        GridUtil.addRow(grid, trans, fill, _fillColor);
        box.addChild(grid);

        var bar :ButtonBar = new ButtonBar();
        bar.addChild(new CommandButton(ctx.IMAGE.get("b.cancel"), close));
        bar.addChild(_okBtn = new CommandButton(ctx.IMAGE.get("b.ok"), close, true));
        box.addChild(bar);

        PopUpManager.addPopUp(this, ctx.getApplication(), true);
        PopUpUtil.center(this);
    }

    protected function checkValid (... ignored) :void
    {
        _okBtn.enabled =
            (_widthValidator.validate(null, true).type == ValidationResultEvent.VALID) &&
            (_heightValidator.validate(null, true).type == ValidationResultEvent.VALID);
    }

    protected function close (create :Boolean = false) :void
    {
        PopUpManager.removePopUp(this);

        if (create) {
            // first, figure out the fill
            var fillColor :uint = Boolean(_fillGroup.selectedValue) ?
            (0xFF000000 | _fillColor.selectedColor) : 0x00000000;

            var bmp :BitmapData = new BitmapData(
                ImageUtil.normalizeImageDimension(int(_width.text)),
                ImageUtil.normalizeImageDimension(int(_height.text)), true, fillColor);
            dispatchEvent(new ValueEvent(Event.COMPLETE, bmp));
        }
    }

    protected var _width :TextInput;
    protected var _height :TextInput;

    protected var _widthValidator :NumberValidator;
    protected var _heightValidator :NumberValidator;

    protected var _fillColor :ColorPicker;

    protected var _okBtn :CommandButton;

    protected var _fillGroup :RadioButtonGroup = new RadioButtonGroup();
}
}
