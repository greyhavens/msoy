//
// $Id$

package com.threerings.msoy.applets {

import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.external.ExternalInterface;
import flash.media.Camera;
import flash.text.TextFieldAutoSize;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.SimpleSkinButton;
import com.threerings.flash.TextFieldUtil;

[SWF(width="160", height="19")]
public class CameraButtonApp extends Sprite
{
    public static const HEIGHT :int = 19;

    public function CameraButtonApp ()
    {
        var names :Array = Camera.names;
        if (names != null && names.length > 0) {
            // they have a camera, add the button
            var button :SimpleSkinButton = new SimpleSkinButton(BUTTON_SKIN,
                "Take a webcam snapshot",
                { autoSize: TextFieldAutoSize.CENTER },
                { font: "_sans", color: 0, size: 12 },
                8, HEIGHT, 1, 0, 0x404040);
            button.addEventListener(MouseEvent.CLICK, handleClick);
            addChild(button);
        }

        ParameterUtil.getParameters(this, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        _mediaIds = String(params["mediaIds"]);
    }

    protected function handleClick (event :MouseEvent) :void
    {
        try {
            ExternalInterface.call("takeSnapshot", _mediaIds);
        } catch (e :Error) {
            trace(e);
        }
    }

    protected var _mediaIds :String;

    [Embed(source="../../../../../../pages/images/ui/button_middle.png")]
    protected static const BUTTON_SKIN :Class;
}
}
