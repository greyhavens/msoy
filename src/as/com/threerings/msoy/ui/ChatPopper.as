package com.threerings.msoy.ui {

import flash.display.BitmapData;

import flash.events.TimerEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.utils.Timer;

import mx.core.IFlexDisplayObject;
import mx.core.UIComponent;

import mx.containers.Box;
import mx.containers.Canvas;

import mx.effects.Fade;
import mx.effects.Parallel;
import mx.effects.Sequence;
import mx.effects.Zoom;

import mx.managers.PopUpManager;

import com.threerings.util.DisplayUtil;

import com.threerings.crowd.chat.data.ChatMessage;

public class ChatPopper
{
    public static function setChatView (view :UIComponent) :void
    {
        _view = view;
        _bounds.topLeft = view.localToGlobal(new Point());
        _bounds.width = view.width;
        _bounds.height = view.height;
    }

    public static function popUp (
            msg :ChatMessage, speaker :Avatar = null) :void
    {
        var bubble :ChatBubble;
        if (speaker != null) {
            bubble = speaker.createChatBubble();

        } else {
            bubble = new ChatBubble();
        }
        bubble.setMessage(msg);

        // add the bubble briefly so that we can measure/layout the text
        PopUpManager.addPopUp(bubble, _view);
        bubble.validateNow();
        PopUpManager.removePopUp(bubble);

        // now we know the size and can try positioning the bubble
        var rect :Rectangle = new Rectangle();
        rect.width = bubble.width;
        rect.height = bubble.height;

        if (speaker != null) {
            // position it near the speaker's head
            var p :Point = speaker.localToGlobal(new Point());
            rect.x = p.x + (speaker.width - rect.width)/2;
            rect.y = p.y;

        } else {
            // position it in the upper corner
            rect.x = 0;
            rect.y = 0;
        }

        // now avoid all the other rectangles (with padding)
        var avoid :Array = new Array();
        /*
        for each (var b :IFlexDisplayObject in _bubbles) {
            avoid.push(new Rectangle(b.x - PAD, b.y - PAD,
                b.width + PAD*2, b.height + PAD*2));
        }
        */
        for each (var arect :Rectangle in _rects) {
            avoid.push(new Rectangle(arect.x - PAD, arect.y - PAD,
                arect.width + PAD*2, arect.height + PAD*2));
        }

        // position it
        DisplayUtil.positionRect(rect, _bounds, avoid);

        var src :BitmapData =
            new BitmapData(bubble.width, bubble.height, true, 0xFF00FF);
        src.draw(bubble);
        var bmp :Bitmap = new Bitmap(src);
        var bubbleViz :Canvas = new Canvas();
        bubbleViz.rawChildren.addChild(bmp);

        bubbleViz.x = rect.x;
        bubbleViz.y = rect.y;
        bubbleViz.width = rect.width;
        bubbleViz.height = rect.height;

        animateBubblePopup(bubbleViz, speaker);

        // track it
        _bubbles.push(bubbleViz);
        _rects.push(rect);

        var timer :Timer = new Timer(10000, 1);
        timer.addEventListener(TimerEvent.TIMER,
            function (evt :TimerEvent) :void
            {
                if (bubbleViz.parent != null) {
                    animateBubblePopdown(bubbleViz);
                }
            });
        timer.start();
    }

    public static function popAllDown () :void
    {
        while (_bubbles.length > 0) {
            var b :IFlexDisplayObject = (_bubbles.pop() as IFlexDisplayObject);
            PopUpManager.removePopUp(b);
        }
        _rects.length = 0;
        // this will leave some dangling Timers, but they'll just cope
        // that their bubble is gone
    }

    protected static function animateBubblePopup (
            bubble :IFlexDisplayObject, speaker :Avatar) :void
    {
        // TODO: this can be pulled into a utility class, and be
        // configurable based on the speaker, etc
        PopUpManager.addPopUp(bubble, _view);

        var w :Number = bubble.width;
        var h :Number = bubble.height;

        bubble.scaleX = .01;
        bubble.scaleY = .01;

        var zoomIn :Zoom = new Zoom(bubble);
        zoomIn.originX = w/2 + 5;
        zoomIn.originY = h/2 + 5;
        zoomIn.duration = 180;
        zoomIn.zoomHeightFrom = .01;
        zoomIn.zoomHeightTo = 1.1;
        zoomIn.zoomWidthFrom = .01;
        zoomIn.zoomWidthTo = 1.1;

        var zoomOut :Zoom = new Zoom(bubble);
        zoomOut.originX = w/2 + 5;
        zoomOut.originY = h/2 + 5;
        zoomOut.duration = 20;
        zoomOut.zoomHeightFrom = 1.1;
        zoomOut.zoomHeightTo = 1;
        zoomOut.zoomWidthFrom = 1.1;
        zoomOut.zoomWidthTo = 1;

        var seq :Sequence = new Sequence(bubble);
        seq.addChild(zoomIn);
        seq.addChild(zoomOut);
        seq.play();
    }

    protected static function animateBubblePopdown (
            bubble :IFlexDisplayObject) :void
    {
        // maybe we don't make this custom: bubbles always just fade out

        var fadeOut :Fade = new Fade(bubble);
        fadeOut.alphaFrom = 1.0;
        fadeOut.alphaTo = 0;
        fadeOut.duration = 750;

        var goAway :FunctionEffect = new FunctionEffect(bubble);
        goAway.func = function () :void {
            // remove it
            PopUpManager.removePopUp(bubble);

            // stop tracking it
            var idx :int = _bubbles.indexOf(bubble); // ref equality
            if (idx != -1) {
                _bubbles.splice(idx, 1);
                _rects.splice(idx, 1);
            }
        };

        var seq :Sequence = new Sequence(bubble);
        seq.addChild(fadeOut);
        seq.addChild(goAway);
        seq.play();
    }

    protected static var _view :UIComponent;

    /** The current bubbles on the screen. */
    protected static var _bubbles :Array = new Array();

    protected static var _bounds :Rectangle = new Rectangle();

    protected static var _rects :Array = new Array();

    protected static const PAD :int = 10;
}
}
