package com.threerings.msoy.world.chat.client {

import flash.display.Bitmap;
import flash.display.BitmapData;

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
    /**
     * Factory method to create a ChatBubble of the specified type.
     */
    public static function createInstance (bubStyle :int) :ChatBubble
    {
        var BUBBLE_TYPES :Array = [ ChatBubble, BloodChatBubble ];
        var clazz :Class = (BUBBLE_TYPES[bubStyle] as Class);
        if (clazz == null) {
            clazz = ChatBubble;
        }
        return (new clazz() as ChatBubble);
    }

    public function ChatBubble ()
    {
        _txt = new Text();
        _txt.addEventListener(FlexEvent.UPDATE_COMPLETE, textWasMeasured);
        addChild(_txt);
    }

    /**
     * Set the message to be displayed.
     */
    public final function setMessage (
            msg :ChatMessage, layoutFinished :Function) :void
    {
        _layoutFinished = layoutFinished;
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
    protected function configureViz (ww :Number, hh :Number) :UIComponent
    {
        drawBackground(ww, hh);
        validateNow();

        var src :BitmapData =
            new BitmapData(this.width, this.height, true, 0xFF00FF);
        src.draw(this);
        bitmapCreated(src);
        var bmp :Bitmap = new Bitmap(src);
        var bubbleViz :Canvas = new Canvas();
        bubbleViz.rawChildren.addChild(bmp);
        return bubbleViz;
    }

    protected function drawBackground (ww :Number, hh :Number) :void
    {
        // draw bubble stuff behind it
        graphics.clear();
        graphics.beginFill(0xFFFFFF);
        graphics.drawRoundRect(0, 0, ww + 20, hh + 20, 10, 10);
        graphics.endFill();
        graphics.lineStyle(2, 0x000000);
        graphics.drawRoundRect(0, 0, ww + 20, hh + 20, 10, 10);
    }

    protected function bitmapCreated (src :BitmapData) :void
    {
        // nada
    }

    private function textWasMeasured (evt :FlexEvent) :void
    {
        _txt.removeEventListener(FlexEvent.UPDATE_COMPLETE, textWasMeasured);

        _layoutFinished(configureViz(_txt.measuredWidth, _txt.measuredHeight));
        _layoutFinished = null; // gc
    }

    protected var _txt :Text;

    protected var _layoutFinished :Function;
}
}
