package com.threerings.msoy.ui {

import flash.events.Event;

import flash.geom.Point;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import flash.display.Sprite;

import mx.containers.Canvas;
import mx.controls.Text;

import mx.core.Container;
import mx.core.UIComponent;
import mx.core.UITextField;

import mx.events.FlexEvent;

import com.threerings.crowd.chat.data.ChatMessage;

public class ChatBubble extends Canvas
{
    public function ChatBubble ()
    {
        _txt.addEventListener(FlexEvent.UPDATE_COMPLETE, textWasMeasured);
        addChild(_txt);
    }

    /**
     * Set the message to be displayed.
     */
    public final function setMessage (msg :ChatMessage) :void
    {
        configureTextStyle();
        _txt.text = msg.message;
    }

    /**
     * Set up and configure the style and look and feel of the text
     * widget that will hold the chat text.
     */
    protected function configureTextStyle () :void
    {
        _txt.styleName = "chatBubble";

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
        _txt.selectable = false;
        _txt.maxWidth = 400;

        //addChild(txt);
    }

    /**
     * Configure the chat bubble decoration.
     */
    protected function configureDecoration (ww :Number, hh :Number) :void
    {
        // draw bubble stuff behind it
        graphics.clear();
        graphics.beginFill(0xFFFFFF);
        graphics.drawRoundRect(0, 0, ww + 20, hh + 20, 10, 10);
        graphics.endFill();
        graphics.lineStyle(2, 0x000000);
        graphics.drawRoundRect(0, 0, ww + 20, hh + 20, 10, 10);
    }

    private function textWasMeasured (evt :FlexEvent) :void
    {
        _txt.removeEventListener(FlexEvent.UPDATE_COMPLETE, textWasMeasured);

        configureDecoration(_txt.measuredWidth, _txt.measuredHeight);
    }
    protected var _txt :Text = new Text();
}
}
