package com.threerings.msoy.ui {

import mx.controls.Label;

/**
 * Pointless utility methods.
 */
public class MsoyUI
{
    public static function createLabel (txt :String) :Label
    {
        var label :Label = new Label();
        label.text = txt;
        return label;
    }
}
}
