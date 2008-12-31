package com.threerings.msoy.client {

import flash.display.BlendMode;
import flash.display.Sprite;

import flash.events.MouseEvent;

import flash.geom.Point;

import flash.text.AntiAliasType;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import caurina.transitions.Tweener;

import com.threerings.msoy.utils.TextUtil;

import com.threerings.msoy.chat.client.ChatOverlay;

/**
 * Simple notification bubble to draw the user's attention to something in the interface.
 */
public class BubblePopup extends Sprite
{
    /**
     * Fades in a bubble as an overlay and fades it out again. If the user hovers over it, it fades
     * back in. If the user clicks it, it goes away.
     * @param parent the place box to overlay the bubble
     * @param str the string to show in the bubble
     * @param gloc the global location where the tail of the bubble should be
     */
    public static function showHelpBubble (
        parent :PlaceBox, str :String, gloc :Point) :void
    {
        // convert to local coordinates
        var loc :Point = parent.globalToLocal(gloc);
        // TODO: orient bubble tail based on the edge of the container the new point is closest to
        var bubble :BubblePopup = new BubblePopup(str);
        bubble.x = loc.x;
        bubble.y = loc.y;
        bubble.alpha = 0;
        parent.addOverlay(bubble, PlaceBox.LAYER_HELP_BUBBLES);

        // fade in and fade out if there is no other user input
        Tweener.addTween(bubble, {alpha :1.0, time :FADE_IN_DURATION});
        fadeOut(SOLID_DURATION + FADE_IN_DURATION);

        // mouse over makes solid and stops all fades
        bubble.addEventListener(MouseEvent.MOUSE_OVER, function (evt :MouseEvent) :void {
            Tweener.removeTweens(bubble);
            bubble.alpha = 1.0;
        });

        // mouse out restarts the fade
        bubble.addEventListener(MouseEvent.MOUSE_OUT, function (evt :MouseEvent) :void {
            Tweener.removeTweens(bubble);
            fadeOut(SOLID_DURATION);
        });

        // click dismisses
        bubble.addEventListener(MouseEvent.CLICK, function (evt :MouseEvent) :void {
            Tweener.removeTweens(bubble);
            remove();
        });

        // helper to fade out the bubble
        function fadeOut (delay :Number) :void {
            Tweener.addTween(bubble, {alpha :0.0, time :FADE_OUT_DURATION,
                delay :delay, onComplete :remove});
        }

        // helper to remove the bubble from the place box
        function remove () :void {
            parent.removeOverlay(bubble);
        }
    }

    /**
     * Creates a new notification bubble.
     */
    public function BubblePopup (str :String)
    {
        // NB: 0, 0 is the tip of the tail

        var format :TextFormat = ChatOverlay.createChatFormat();
        var text :TextField = new TextField();
        text.multiline = true;
        text.wordWrap = true;
        text.selectable = false;
        text.autoSize = TextFieldAutoSize.LEFT;
        text.antiAliasType = AntiAliasType.ADVANCED;
        TextUtil.setText(text, TextUtil.parseLinks(str, format, true, true), format);
        text.width = WIDTH - PADDING * 2;
        addChild(text);

        // position just inside the bubble
        text.x = -TAIL_TIP_X + PADDING;
        text.y = -TAIL_HEIGHT - PADDING - text.height;

        // rectangle outline
        graphics.lineStyle(1, OUTLINE);
        graphics.beginFill(FILL);
        graphics.drawRoundRect(-TAIL_TIP_X, -TAIL_HEIGHT - PADDING * 2 - text.height, WIDTH,
            text.height + PADDING * 2, ROUNDING);
        graphics.endFill();

        // tail interior
        graphics.lineStyle(1, FILL);
        graphics.beginFill(FILL);
        graphics.drawRect(-TAIL_BASE_X + 1, -TAIL_HEIGHT - PADDING / 2, TAIL_BASE_WIDTH - 2,
            PADDING / 2);
        drawTailWedge();
        graphics.endFill();

        // tail exterior
        graphics.lineStyle(1, OUTLINE);
        drawTailWedge();

        // make sure our children get the alpha setting too (contrary to flash documentation)
        blendMode = BlendMode.LAYER;
    }

    protected function drawTailWedge () :void
    {
        graphics.moveTo(-TAIL_BASE_X, -TAIL_HEIGHT);
        graphics.lineTo(0, 0);
        graphics.lineTo(-TAIL_BASE_X + TAIL_BASE_WIDTH, -TAIL_HEIGHT);
    }

    protected static const WIDTH :int = 160;
    protected static const PADDING :int = 5;
    protected static const ROUNDING :int = 12;
    protected static const OUTLINE :int = 0x000000;
    protected static const FILL :int = 0xffffff;

    protected static const TAIL_BASE_X :int = 10;
    protected static const TAIL_BASE_WIDTH :int = 20;
    protected static const TAIL_TIP_X :int = 30;
    protected static const TAIL_HEIGHT :int = 18;

    protected static const FADE_IN_DURATION :Number = 0.75;
    protected static const SOLID_DURATION :Number = 3.0;
    protected static const FADE_OUT_DURATION :Number = 2.0;
}

}
