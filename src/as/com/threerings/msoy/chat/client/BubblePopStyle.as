package com.threerings.msoy.chat.client {

import mx.core.IFlexDisplayObject;

import mx.effects.Fade;
import mx.effects.Parallel;
import mx.effects.Rotate;
import mx.effects.Sequence;
import mx.effects.Zoom;

public class BubblePopStyle
{
    public static function animateBubble (
            bubble :IFlexDisplayObject, popStyle :int) :void
    {
        switch (popStyle) {
        default:
            overZoom(bubble);
            break;

        case 1:
            rotateFade(bubble);
            break;
        }
    }

    private static function rotateFade (bubble :IFlexDisplayObject) :void
    {
        bubble.alpha = 0;

        var w :Number = bubble.width;
        var h :Number = bubble.height;

        var rot :Rotate = new Rotate(bubble);
        rot.angleFrom = 90;
        rot.angleTo = 360;
        rot.originX = w/2;
        rot.originY = h/2;
        rot.duration = 200;

        var fade :Fade = new Fade(bubble);
        fade.alphaFrom = .25;
        fade.alphaTo = 1.0;
        fade.duration = 200;

        var par :Parallel = new Parallel(bubble);
        par.addChild(rot);
        par.addChild(fade);
        par.play();
    }

    private static function overZoom (bubble :IFlexDisplayObject) :void
    {
        var zoomIn :Zoom = new Zoom(bubble);
        zoomIn.duration = 180;
        zoomIn.zoomHeightFrom = .01;
        zoomIn.zoomHeightTo = 1.1;
        zoomIn.zoomWidthFrom = .01;
        zoomIn.zoomWidthTo = 1.1;

        var zoomOut :Zoom = new Zoom(bubble);
        zoomOut.duration = 20;
        zoomOut.zoomHeightFrom = 1.1;
        zoomOut.zoomHeightTo = 1;
        zoomOut.zoomWidthFrom = 1.1;
        zoomOut.zoomWidthTo = 1;

        var seq :Sequence = new Sequence(bubble);
        seq.addChild(zoomIn);
        seq.addChild(zoomOut);
        seq.play();

        // set the initial scale now, otherwise the object may appear
        // for one frame at the full size.
        bubble.scaleX = .01;
        bubble.scaleY = .01;
    }
}
}
