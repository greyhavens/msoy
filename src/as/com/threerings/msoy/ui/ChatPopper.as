package com.threerings.msoy.ui {


import flash.events.TimerEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.utils.Timer;

import mx.core.UIComponent;

import mx.managers.PopUpManager;

import com.threerings.util.DisplayUtil;

import com.threerings.crowd.chat.data.ChatMessage;

public class ChatPopper
{
    public static function setChatView (view :UIComponent) :void
    {
        _view = view;
        _bounds.width = view.width;
        _bounds.height = view.height;
        _bounds.topLeft = view.localToGlobal(new Point());
    }

    public static function popUp (
            msg :ChatMessage, speaker :Avatar = null) :void
    {
        var bubble :ChatBubble = new ChatBubble(msg);

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

        // now avoid all the other rectangles
        var avoid :Array = new Array();
        for each (var bub :ChatBubble in _bubbles) {
            avoid.push(new Rectangle(bub.x, bub.y, bub.width, bub.height));
        }

        // position it and pop it up
        DisplayUtil.positionRect(rect, _bounds, avoid);
        bubble.x = rect.x;
        bubble.y = rect.y;
        PopUpManager.addPopUp(bubble, _view);

        // track it
        _bubbles.push(bubble);

        var timer :Timer = new Timer(10000, 1);
        timer.addEventListener(TimerEvent.TIMER,
            function (evt :TimerEvent) :void
            {
                var idx :int = _bubbles.indexOf(bubble); // reference equality
                if (idx != -1) {
                    _bubbles.splice(idx, 1);
                    PopUpManager.removePopUp(bubble);
                }
            });
        timer.start();
    }

    public static function popAllDown () :void
    {
        while (_bubbles.length > 0) {
            var bub :ChatBubble = (_bubbles.pop() as ChatBubble);
            PopUpManager.removePopUp(bub);
        }
        // this will leave some dangling Timers, but they'll just cope
        // that their bubble is gone
    }

    protected static var _view :UIComponent;

    /** The current bubbles on the screen. */
    protected static var _bubbles :Array = new Array();

    protected static var _bounds :Rectangle = new Rectangle();
}
}
