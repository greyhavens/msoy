//
// $Id$

package tutorial {

import flash.display.*;
import flash.text.*;
import flash.geom.*;
import flash.events.*;
import flash.filters.*;
import flash.net.*;
import flash.ui.*;
import flash.utils.*;

import com.threerings.util.EmbeddedSwfLoader;
import com.threerings.flash.SimpleTextButton;

public class TextBox extends Sprite
{
    public function TextBox (view :View, swirlBytes :ByteArray, done :Function)
    {
        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        loader.addEventListener(Event.COMPLETE, handleTextboxLoaded);
        loader.load(swirlBytes);

        _view = view;
        _done = done;

        var styleSheet :StyleSheet = new StyleSheet();
        styleSheet.parseCSS(
            "body {" +
            "  color: #000000;" +
            "}" +
            ".title {" +
            "  font-family: SunnySide;" +
            "  font-size: 20;" +
            "  text-decoration: underline;" +
            "  text-align: center;" +
            "}" +
            ".summary {" +
            "  font-family: Goudy;" +
            "  font-weight: bold;" +
            "  font-size: 16;" +
            "  text-align: center;" +
            "}" +
            ".details {" +
            "  font-family: Goudy;" +
            "  font-size: 14;" +
            "  text-align: left;" +
            "}");

        _textField = new TextField();
        _textField.border = false;
        _textField.borderColor = 0xFF0000;
        _textField.defaultTextFormat = getDefaultFormat();
        _textField.styleSheet = styleSheet;
        _textField.wordWrap = true;
        _textField.multiline = true;
        _textField.embedFonts = true;
        _textField.antiAliasType = flash.text.AntiAliasType.ADVANCED;
        _textField.autoSize = TextFieldAutoSize.CENTER;
        _textField.width = 400;

        // we start off invisible
        this.visible = false;
    }

    public function unload () :void
    {
    }

    protected function handleTextboxLoaded (evt :Event) :void
    {
        // create the textbox clip
        var boxClip :MovieClip = MovieClip(EmbeddedSwfLoader(evt.target).getContent());
        boxClip.gotoAndStop(1, SCN_TEXTBOX);

        boxClip.x = -Content.BOX_OFFSET.x;
        boxClip.y = -Content.BOX_OFFSET.y;

        _backdrop = new Sprite();
        _backdrop.addChild(boxClip);

        this.addChild(_backdrop);

        _textField.x = _backdrop.x + Content.BOX_PADDING;
        _textField.y = _backdrop.y + Content.BOX_PADDING;
        this.addChild(_textField);

        _buttons = new Sprite();
        _buttons.x = _textField.x;
        this.addChild(_buttons);

        _done();
    }

    public function isReady () :Boolean
    {
        return !! _buttons;
    }

    public function newBox (text :String) :TextField
    {
        while (_buttons.numChildren > 0) {
            _buttons.removeChildAt(0);
        }
        _rightButtonEdge = _textField.width;
        _leftButtonEdge = 0;

        _textField.htmlText = text;

        _backdrop.width = _textField.width + 2*Content.BOX_PADDING;
        _backdrop.height = _textField.height + Content.BOX_HAT + 2*Content.BOX_PADDING;

        _buttons.y = _textField.y + _textField.height;

        this.visible = true;

        return _textField;
    }

    public function addButton (label :String, right :Boolean, onClick :Function) :SimpleButton
    {
        var button :SimpleButton = new SimpleTextButton(
            label, true, 0x003366, 0x6699CC, 0x0066FF, 5, getDefaultFormat());
        button.addEventListener(MouseEvent.CLICK, function (evt :Event) :void {
                onClick();
            });
        _buttons.addChild(button);
        if (right) {
            button.x = _rightButtonEdge - button.width;
            _rightButtonEdge -= button.width + Content.BOX_PADDING;
        } else {
            button.x = _leftButtonEdge;
            _leftButtonEdge += button.width + Content.BOX_PADDING;
        }

        _backdrop.height = _textField.height + _buttons.height +
            Content.BOX_HAT + 2*Content.BOX_PADDING;

        log.debug("Added button at (" + button.x + ", " + button.y + ") while _buttons is at (" + _buttons.x + ", " + _buttons.y + ")");

        return button;
    }

    protected function getDefaultFormat () :TextFormat
    {
        var format :TextFormat = new TextFormat();
        format.font = "SunnySide";
        format.size = 14;
        format.color = 0x000000;
        format.align = TextFormatAlign.LEFT;
        return format;
    }

    protected var _view :View;
    protected var _done :Function;

    protected var _backdrop :Sprite;
    protected var _textField :TextField;

    protected var _buttons :Sprite;
    protected var _leftButtonEdge :int;
    protected var _rightButtonEdge :int;

    protected static const log :Log = Log.getLog(Swirl);

    protected static const SCN_TEXTBOX :String = "textbox";
}
}
