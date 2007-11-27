//
// $Id$

package com.threerings.msoy.ui {

import flash.events.Event;
import flash.events.TextEvent;

import mx.controls.Label;
import mx.controls.TextInput;

/**
 * Pointless utility methods.
 */
public class MsoyUI
{
    public static function createLabel (txt :String, style :String = null) :Label
    {
        var label :Label = new Label();
        label.text = txt;
        if (style != null) {
            label.styleName = style;
        }
        return label;
    }

//    /**
//     * Enforce the entry of only approved strings. See also
//     * the 'restrict' property of TextInput.
//     *
//     * @param fn function (str :String) :Boolean indicates complience.
//     */
//    // TODO: Fuck me, this should be ripped out in favor of Validators,
//    // TODO: look into that.
//    public static function enforce (input :TextInput, fn :Function) :void
//    {
//        input.addEventListener(TextEvent.TEXT_INPUT,
//            function (evt :TextEvent) :void {
//                var s :String =
//                    input.text.substring(0, input.selectionBeginIndex) +
//                    evt.text + input.text.substr(input.selectionEndIndex);
//                if (!fn(s)) {
//                    evt.preventDefault();
//                }
//            }, false, int.MAX_VALUE);
//    }
//
//    /**
//     * Enforce the entry of only numbers.
//     */
//    public static function enforceNumber (input :TextInput) :void
//    {
//        enforce(input, isNumber);
//    }
//
//    /**
//     * A handy function to test if a string is parseable as a Number.
//     * This function allows "-" (equal to negative 0),
//     * and prevents having "e" in the middle of the value, which is
//     * normally acceptable. Make your own function if you don't like it.
//     */
//    public static function isNumber (s :String) :Boolean
//    {
//        if (s == "-") {
//            return true;
//        }
//        if (s.indexOf("e") > -1) {
//            return false;
//        }
//        return !isNaN(Number(s));
//    }
}
}
