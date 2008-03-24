//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.utils.ByteArray;

import mx.controls.ButtonBar;

import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.core.Application;

import mx.managers.PopUpManager;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.PopUpUtil;

import com.threerings.msoy.applets.image.ImageManipulator;

/**
 * Dispatched when the image is saved.
 * Value: [ imageByteArray, <optional: forced extension> ]
 */
[Event(name="ImageUpdated", type="com.threerings.util.ValueEvent")]

public class PopupImageEditor extends TitleWindow
{
    public static const IMAGE_UPDATED :String = "ImageUpdated";

    public function PopupImageEditor (
        bytes :ByteArray, cutWidth :Number = NaN, cutHeight :Number = NaN)
    {
        var box :VBox = new VBox();
        addChild(box);

        _image = new ImageManipulator(true, cutWidth, cutHeight);
        _image.maxWidth = 450;
        _image.maxHeight = 300;
        _image.minWidth = 200;
        _image.minHeight = 100;
        box.addChild(_image);

        var bar :ButtonBar = new ButtonBar();
        bar.addChild(new CommandButton("Save", close, true));
        bar.addChild(new CommandButton("Cancel", close, false));
        box.addChild(bar);

        _image.setImage(bytes);

        PopUpManager.addPopUp(this, Application(Application.application), true);
        PopUpUtil.center(this);
    }

    protected function close (save :Boolean) :void
    {
        if (save) {
            dispatchEvent(new ValueEvent(IMAGE_UPDATED, _image.getImage()));
        }

        PopUpManager.removePopUp(this);
    }

    protected var _image :ImageManipulator;
}
}
