package com.threerings.msoy.ui {

import flash.events.Event;
import flash.events.TimerEvent;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import flash.display.Sprite;

import flash.utils.Timer;

import mx.core.Container;

import com.threerings.crowd.chat.data.ChatMessage;

public class ChatBubble extends Sprite
{
    public function ChatBubble (avatar :Avatar, msg :ChatMessage)
    {
        var txt :TextField = new TextField();
        _txt = txt;
        //txt.background = true;
        //txt.backgroundColor = 0xFFFFFF;
        txt.textColor = 0x000000;
        txt.selectable = false;
        txt.restrict = "";
        txt.wordWrap = true;
        txt.multiline = true;
        txt.x = 5; 
        txt.y = 5;
        txt.autoSize = TextFieldAutoSize.CENTER;
        txt.text = msg.message;

        trace("text size is " + txt.width + "x" + txt.height);
        addEventListener(Event.ADDED, wasAdded);

        // draw bubble stuff behind it
        graphics.clear();
        graphics.beginFill(0xFFFFFF);
        graphics.drawRoundRect(0, 0, txt.width + 20, txt.height + 20, 10, 10);
        graphics.endFill();
        graphics.lineStyle(2, 0x000000);
        graphics.drawRoundRect(0, 0, txt.width + 20, txt.height + 20, 10, 10);

        addChild(txt);

        // TODO properly position around the avatar
        this.x = 10;
        this.y = 40;

        var timer :Timer = new Timer(10000, 1);
        timer.addEventListener(TimerEvent.TIMER, popDown);
        timer.start();
    }

    protected function popDown (evt :TimerEvent) :void
    {
        (parent as Container).rawChildren.removeChild(this);
    }

    protected function wasAdded (evt :Event) :void
    {
        trace("text size is " + _txt.width + "x" + _txt.height);
        removeEventListener(Event.ADDED, wasAdded);

        _txt.autoSize = TextFieldAutoSize.NONE;
    }

    protected var _txt :TextField;
}
}
