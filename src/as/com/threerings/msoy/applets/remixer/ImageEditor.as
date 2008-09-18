//
// $Id$

package com.threerings.msoy.applets.remixer {

import mx.containers.VBox;

import com.threerings.util.ValueEvent;

import com.threerings.msoy.applets.image.ImageManipulator;
import com.threerings.msoy.applets.image.SizeRestriction;

/**
 * Dispatched when the editor is closed.
 * Value: null for no change, or [ imageByteArray, <optional: forced extension> ]
 */
[Event(name="ImageUpdated", type="com.threerings.util.ValueEvent")]

public class ImageEditor extends VBox
{
    public static const IMAGE_UPDATED :String = "ImageUpdated";

    public function ImageEditor (
        ctx :RemixContext, image :Object, sizeRestrict :SizeRestriction = null)
    {
        _ctx = ctx;

        _image = new ImageManipulator(ctx, ctx.getViewWidth(), ctx.getViewHeight(), sizeRestrict);
        addChild(_image);

        _image.addEventListener(ImageManipulator.CLOSE, handleClosed);
        _image.setImage(image);

        _ctx.pushView(this);
    }

    protected function handleClosed (event :ValueEvent) :void
    {
        _ctx.popView();

        dispatchEvent(new ValueEvent(IMAGE_UPDATED, event.value ? _image.getImage() : null));
    }

    protected var _ctx :RemixContext;

    protected var _image :ImageManipulator;
}
}
