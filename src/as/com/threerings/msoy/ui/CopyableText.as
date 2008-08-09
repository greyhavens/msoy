package com.threerings.msoy.ui {

import flash.system.System;
import flash.events.FocusEvent;

import mx.containers.HBox;
import mx.controls.TextInput;

import com.threerings.flex.CommandButton;

/** A handy group of widgets for stuffing something into your clipboard. */
public class CopyableText extends HBox
{
    /**
     * If maybeTextInput is a TextInput, use it... otherwise use it as the text of a new TextInput.
     */
    public function CopyableText (maybeTextInput :Object)
    {
        var field :TextInput;

        // Overloaded constructors for AS4?
        if (maybeTextInput is TextInput) {
            field = maybeTextInput as TextInput;
        } else {
            field = new TextInput();
            field.text = String(maybeTextInput);
        }

        percentWidth = 100;

        field.editable = false;
        field.percentWidth = 100;

        // On focus, select all
        field.addEventListener(FocusEvent.FOCUS_IN, function (... ignored) :void {
            field.selectionBeginIndex = 0;
            field.selectionEndIndex = field.text.length;
        });

        addChild(field);

        var button :CommandButton = new CommandButton(null, function () :void {
            System.setClipboard(field.text);
        });
        button.styleName = "copyButton";

        addChild(button);
        defaultButton = button;
    }
}

}
