package com.threerings.msoy.ui {

import flash.events.Event;
import flash.events.TimerEvent;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import flash.display.Sprite;

import flash.utils.Timer;

import mx.containers.Canvas;
import mx.controls.Text;

import mx.core.Container;
import mx.core.UIComponent;

import mx.effects.Fade;
import mx.effects.Parallel;
import mx.effects.RemoveChildAction;
import mx.effects.Sequence;
import mx.effects.Zoom;

import mx.events.FlexEvent;

import mx.styles.StyleManager;

import com.threerings.crowd.chat.data.ChatMessage;

public class ChatBubble extends Canvas
{
    public function ChatBubble (avatar :Avatar, msg :ChatMessage)
    {
        var txt :Text = new Text();
        txt.styleName = "chatBubble";

        /*
        var txt :TextField = new TextField();
        txt.background = true;
        txt.backgroundColor = 0xFFFFFF;
        txt.textColor = 0x000000;
        txt.restrict = "";
        txt.wordWrap = true;
        txt.multiline = true;
        txt.autoSize = TextFieldAutoSize.CENTER;
        */
        txt.selectable = false;
        txt.x = 0; 
        txt.y = 0;
        txt.text = msg.message;

        txt.addEventListener(FlexEvent.UPDATE_COMPLETE, textWasMeasured);
        addChild(txt);

        // TODO: proper positioning, right now we're depending
        // on our parent being a Box

        var timer :Timer = new Timer(10000, 1);
        timer.addEventListener(TimerEvent.TIMER, popDown);
        timer.start();
    }

    protected function popDown (evt :TimerEvent) :void
    {
        var fadeOut :Fade = new Fade(this);
        fadeOut.alphaFrom = 1.0;
        fadeOut.alphaTo = 0;
        fadeOut.duration = 750;

        var remove :RemoveChildAction = new RemoveChildAction(this);

        var seq :Sequence = new Sequence(this);
        seq.addChild(fadeOut);
        seq.addChild(remove);

        seq.play();
    }

    protected function textWasMeasured (evt :FlexEvent) :void
    {
        var txt :Text = (evt.currentTarget as Text);
        var w :Number = txt.measuredWidth;
        var h :Number = txt.measuredHeight;

        txt.removeEventListener(FlexEvent.UPDATE_COMPLETE, textWasMeasured);

        // draw bubble stuff behind it
        graphics.clear();
        graphics.beginFill(0xFFFFFF);
        graphics.drawRoundRect(0, 0, w + 20, h + 20, 10, 10);
        graphics.endFill();
        graphics.lineStyle(2, 0x000000);
        graphics.drawRoundRect(0, 0, w + 20, h + 20, 10, 10);

        var zoomIn :Zoom = new Zoom(this);
        zoomIn.originX = w/2 + 5;
        zoomIn.originY = h/2 + 5;
        zoomIn.duration = 80;
        zoomIn.zoomHeightFrom = 1;
        zoomIn.zoomHeightTo = 1.1;
        zoomIn.zoomWidthFrom = 1;
        zoomIn.zoomWidthTo = 1.1;

        var zoomOut :Zoom = new Zoom(this);
        zoomOut.originX = w/2 + 5;
        zoomOut.originY = h/2 + 5;
        zoomOut.duration = 20;
        zoomOut.zoomHeightFrom = 1.1;
        zoomOut.zoomHeightTo = 1;
        zoomOut.zoomWidthFrom = 1.1;
        zoomOut.zoomWidthTo = 1;

        var seq :Sequence = new Sequence(this);
        seq.addChild(zoomIn);
        seq.addChild(zoomOut);
        seq.play();
    }
}
}
