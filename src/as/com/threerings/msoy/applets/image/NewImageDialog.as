//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.BitmapData;

import flash.events.Event;

import flash.geom.Point;

import flash.utils.ByteArray;

import mx.controls.ButtonBar;
import mx.controls.ColorPicker;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;
import mx.controls.Spacer;
import mx.controls.TextInput;

import mx.containers.Grid;
import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.core.Application;
import mx.core.UIComponent;

import mx.managers.PopUpManager;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;
import com.threerings.flex.PopUpUtil;

public class NewImageDialog extends TitleWindow
{
    public function NewImageDialog (forcedSize :Point = null)
    {
        title = "Create new image";

        var box :VBox = new VBox();
        addChild(box);

        _width = new TextInput();
        _height = new TextInput();
        _width.restrict = "0-9";
        _height.restrict = "0-9";
        _width.maxChars = 4;
        _height.maxChars = 4;
        _width.maxWidth = 100;
        _height.maxWidth = 100;

        if (forcedSize != null) {
            _width.text = String(forcedSize.x);
            _height.text = String(forcedSize.y);
            _width.enabled = false;
            _height.enabled = false;
        } else {
            _width.text = "200";
            _height.text = "200";
        }

        _fillColor = new ColorPicker();
        _fillColor.selectedColor = 0xFFFFFF;
        var trans :RadioButton = new RadioButton();
        trans.label = "Transparent";
        trans.value = false;
        trans.selected = true;
        trans.group = _fillGroup;

        var fill :RadioButton = new RadioButton();
        fill.label = "Fill";
        fill.value = true;
        fill.group = _fillGroup;

        // kinda pointless to use Grids here...
        var grid :Grid = new Grid();
        GridUtil.addRow(grid, "Width:", _width, "Height:", _height);
        box.addChild(grid);

        grid = new Grid();
        GridUtil.addRow(grid, trans, fill, _fillColor);
        box.addChild(grid);

        var bar :ButtonBar = new ButtonBar();
        bar.addChild(new CommandButton("ok", close, true));
        bar.addChild(new CommandButton("Cancel", close));
        box.addChild(bar);

        PopUpManager.addPopUp(this, Application(Application.application), true);
        PopUpUtil.center(this);
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

    protected var _fillColor :ColorPicker;

    protected var _fillGroup :RadioButtonGroup = new RadioButtonGroup();
}
}
