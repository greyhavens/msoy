package com.threerings.msoy.chat.client {

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

import mx.events.ResizeEvent;

import mx.managers.PopUpManager;

import com.threerings.util.DisplayUtil;

import com.threerings.mx.effects.FunctionEffect;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.world.client.AvatarSprite;

public class ChatPopper
{
    public static function setChatView (view :UIComponent) :void
    {
        _view = view;

        _view.addEventListener(ResizeEvent.RESIZE, handleViewResized);
        checkViewSize();
    }

    public static function popUp (
            msg :ChatMessage, speaker :AvatarSprite = null) :void
    {
        var bubble :ChatBubble;
        if (speaker != null) {
            bubble = ChatBubble.createInstance(speaker.getChatStyle());

        } else {
            // default style
            bubble = new ChatBubble();
        }
        bubble.setMessage(msg, function (viz :UIComponent) :void {
            popUp2(viz, bubble, speaker);
        });

        // add the bubble briefly so that we can measure/layout the text
        PopUpManager.addPopUp(bubble, _view);
        bubble.validateNow();
    }

    private static function popUp2 (
            viz :UIComponent, bubble :ChatBubble, speaker :AvatarSprite) :void
    {
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
        for each (var arect :Rectangle in _rects) {
            avoid.push(new Rectangle(arect.x - PAD, arect.y - PAD,
                arect.width + PAD*2, arect.height + PAD*2));
        }

        // position it
        DisplayUtil.positionRect(rect, _bounds, avoid);

        viz.x = rect.x;
        viz.y = rect.y;
        viz.width = rect.width;
        viz.height = rect.height;

        animateBubblePopup(viz, speaker);

        // track it
        _bubbles.push(viz);
        _rects.push(rect);

        var timer :Timer = new Timer(20000, 1);
        timer.addEventListener(TimerEvent.TIMER,
            function (evt :TimerEvent) :void
            {
                if (viz.parent != null) {
                    animateBubblePopdown(viz);
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
            bubble :IFlexDisplayObject, speaker :AvatarSprite) :void
    {
        PopUpManager.addPopUp(bubble, _view);

        var style :int = 0;
        if (speaker != null) {
            style = speaker.getChatPopStyle();
        }
        BubblePopStyle.animateBubble(bubble, style);
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

    protected static function handleViewResized (event :ResizeEvent) :void
    {
        checkViewSize();
    }

    protected static function checkViewSize () :void
    {
        _bounds.topLeft = _view.localToGlobal(new Point());
        _bounds.x += PAD;
        _bounds.y += PAD;
        _bounds.width = _view.width - PAD*2;
        _bounds.height = _view.height - PAD*2;
    }

    protected static var _view :UIComponent;

    /** The current bubbles on the screen. */
    protected static var _bubbles :Array = new Array();

    protected static var _bounds :Rectangle = new Rectangle();

    protected static var _rects :Array = new Array();

    protected static const PAD :int = 10;
}
}
