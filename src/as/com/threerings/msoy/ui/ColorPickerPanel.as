//
// $Id$

package com.threerings.msoy.ui {

import flash.events.Event;

import mx.controls.ColorPicker;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;

import mx.containers.Grid;

import mx.events.ColorPickerEvent;

import mx.binding.utils.BindingUtils;

import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.Prefs;

/**
 * A chincy color picker panel. Ideally this would just pop up off of the menu where it is invoked
 * instead of being a free-floating window, but that is not the way the mx color picker works.
 */
public class ColorPickerPanel extends FloatingPanel
{
    /**
     */
    public function ColorPickerPanel (ctx :MsoyContext)
    {
        super(ctx, Msgs.GENERAL.get("t.frame_color"));
        showCloseButton = true;
        styleName = "colorPickerPanel";
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var useCustom :Boolean = Prefs.getUseCustomBackgroundColor();

        var picker :ColorPicker = new ColorPicker();
        picker.selectedColor = Prefs.getCustomBackgroundColor();
        picker.enabled = useCustom;

        var frameGroup :RadioButtonGroup = new RadioButtonGroup();
        frameGroup.selectedValue = useCustom;

        var defaultBtn :RadioButton = new RadioButton();
        defaultBtn.value = false;
        defaultBtn.label = Msgs.GENERAL.get("b.frame_color_default");
        defaultBtn.group = frameGroup;

        var customBtn :RadioButton = new RadioButton();
        customBtn.value = true;
        customBtn.label = Msgs.GENERAL.get("b.frame_color_custom");
        customBtn.group = frameGroup;

        var grid :Grid = new Grid();
        GridUtil.addRow(grid, defaultBtn);
        GridUtil.addRow(grid, customBtn, picker);
        addChild(grid);

        BindingUtils.bindSetter(Prefs.setUseCustomBackgroundColor, frameGroup, "selectedValue");
        BindingUtils.bindProperty(picker, "enabled", frameGroup, "selectedValue");

        picker.addEventListener(ColorPickerEvent.CHANGE, function (evt :ColorPickerEvent) :void {
            Prefs.setCustomBackgroundColor(evt.color);
        });

        addButtons(OK_BUTTON);
    }
}
}
