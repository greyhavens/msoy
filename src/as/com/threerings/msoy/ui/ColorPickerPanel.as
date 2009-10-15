//
// $Id$

package com.threerings.msoy.ui {

import mx.controls.ColorPicker;
import mx.events.ColorPickerEvent;

import com.threerings.msoy.client.MsoyContext;

/**
 * A chincy color picker panel. Ideally this would just pop up off of the menu where it is invoked
 * instead of being a free-floating window, but that is not the way the mx color picker works.
 */
public class ColorPickerPanel extends FloatingPanel
{
    /**
     * Creates a new color picker panel with the given context, title and starting selection.
     * When the user presses the "select" button, the given function will be called with the
     * selected color.
     * <listing version="3.0">
     * function onSelect (color :uint) :void {}
     * </listing>
     */
    public function ColorPickerPanel (
        ctx :MsoyContext, title :String, selectedColor :uint, onSelect :Function)
    {
        super(ctx, title);
        styleName = "colorPickerPanel";
        _selected = selectedColor;
        _onSelect = onSelect;
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        _picker = new ColorPicker();
        _picker.selectedColor = _selected;
        _picker.addEventListener(ColorPickerEvent.CHANGE, function (evt :ColorPickerEvent) :void {
            _selected = evt.color;
        });
        addChild(_picker);
        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    override protected function getButtonLabel(buttonId :int) :String
    {
        if (buttonId == OK_BUTTON) {
            return "Select Color";
        }
        return super.getButtonLabel(buttonId);
    }

    override protected function okButtonClicked () :void
    {
        _onSelect(_selected);
    }

    protected var _picker :ColorPicker;
    protected var _selected :uint;
    protected var _onSelect :Function;
}
}
