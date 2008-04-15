//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.geom.Point;

import flash.utils.ByteArray;

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

    public function PopupImageEditor (image :Object, forcedSize :Point = null)
    {
        var box :VBox = new VBox();
        addChild(box);

        _image = new ImageManipulator(600, 480, forcedSize);
        box.addChild(_image);

        _image.addEventListener(ImageManipulator.SIZE_KNOWN, handleSizeKnown);
        _image.addEventListener(ImageManipulator.CLOSE, handleClosed);
        _image.setImage(image);

        PopUpManager.addPopUp(this, Application(Application.application), true);
        PopUpUtil.center(this);
    }

    protected function handleClosed (event :ValueEvent) :void
    {
        if (event.value)  {
            dispatchEvent(new ValueEvent(IMAGE_UPDATED, _image.getImage()));
        }

        PopUpManager.removePopUp(this);
    }

    protected function handleSizeKnown (event :ValueEvent) :void
    {
        callLater(PopUpUtil.center, [ this ]);
    }

    protected var _image :ImageManipulator;
}
}
