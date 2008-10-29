//
// $Id$

package com.threerings.msoy.ui {

import flash.system.System;
import flash.events.FocusEvent;

import mx.containers.HBox;
import mx.controls.TextInput;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;

/**
 * A handy group of widgets for stuffing something into your clipboard.
 */
public class CopyableText extends HBox
{
    /**
     * If maybeTextInput is a TextInput, use it... otherwise use it as the text of a new TextInput.
     */
    public function CopyableText (maybeTextInput :Object)
    {
        setStyle("verticalAlign", "middle");
        percentWidth = 100;

        // Overloaded constructors for AS4?
        var field :TextInput;
        if (maybeTextInput is TextInput) {
            field = maybeTextInput as TextInput;
        } else {
            field = new TextInput();
            field.text = String(maybeTextInput);
        }

        field.editable = false;
        field.percentWidth = 100;

        // On focus, select all
        field.addEventListener(FocusEvent.FOCUS_IN, function (... ignored) :void {
            field.selectionBeginIndex = 0;
            field.selectionEndIndex = field.text.length;
        });

        addChild(field);

        var button :CommandButton = new CommandButton(Msgs.GENERAL.get("b.copy"),
            function () :void {
                // we want to copy the text to the clipboard even if it's updated after
                // we set up this binding, so we need this function
                System.setClipboard(field.text);
            });
        button.styleName = "orangeButton";

        addChild(button);
        defaultButton = button;
    }
}
}
