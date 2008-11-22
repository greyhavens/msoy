//
// $Id: NewImageDialog.as 13444 2008-11-21 22:53:54Z ray $

package com.threerings.msoy.applets.image {

import flash.events.Event;

import mx.containers.TitleWindow;

import mx.managers.PopUpManager;

import com.threerings.util.ValueEvent;

import com.threerings.flash.CameraSnapshotter;

import com.threerings.flex.CommandButton;
import com.threerings.flex.PopUpUtil;

public class ImageControlOptions extends TitleWindow
{
    /** Constants for what you want to do with yer images. */
    public static const CANCEL :int = 0;
    public static const NEW :int = 1;
    public static const CAMERA :int = 2;

    public function ImageControlOptions (ctx :ImageContext)
    {
        _ctx = ctx;
        title = ctx.IMAGE.get("t.options");
        showCloseButton = true;
        addEventListener(Event.CLOSE, handleClose);
    }

    public function open () :void
    {
        if (!CameraSnapshotter.hasCamera()) {
            // there's no choice! (well, cancel doesn't count)
            close(NEW);
            return;
        }

        addChild(new CommandButton(_ctx.IMAGE.get("b.new_image"), close, NEW));
        addChild(new CommandButton(_ctx.IMAGE.get("b.camera"), close, CAMERA));

        PopUpManager.addPopUp(this, _ctx.getApplication(), true);
        PopUpUtil.center(this);
    }

    protected function handleClose (... ignored) :void
    {
        close(CANCEL);
    }

    protected function close (option :int = CANCEL) :void
    {
        PopUpManager.removePopUp(this);
        dispatchEvent(new ValueEvent(Event.COMPLETE, option));
    }

    protected var _ctx :ImageContext;
}
}
