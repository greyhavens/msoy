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
import com.threerings.flash.AlphaFade;

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
            ".message {" +
            "  font-family: SunnySide;" +
            "  font-size: 16;" +
            "  text-align: left;" +
            "}" +
            ".details {" +
            "  font-family: Goudy;" +
            "  font-size: 14;" +
            "  text-align: left;" +
            "}");

        _textField = new TextField();
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

    public function isReady () :Boolean
    {
        return _buttons != null;
    }

    public function hideBox () :void
    {
        clearTimer();
        if (_fadeIn.isPlaying()) {
            _fadeIn.stopAnimation();
        }
        _fadeOut.startAnimation();
    }

    public function sizeChanged () :void
    {
        var scale :Number = 500 / Math.max(320, Math.min(500, stage.stageWidth));
        this.scaleX = this.scaleY = scale;

        var offset :Number = Math.min(stage.stageWidth - figureWidth()*scale,
                                      stage.stageHeight - figureHeight()*scale) / 2;

        this.x = Math.max(0, offset);
        this.y = Math.max(Content.BOX_HAT * this.scaleY, offset);
    }

    public function showBox (text :String) :TextField
    {
        clearTimer();
        if (_fadeOut.isPlaying()) {
            _fadeOut.stopAnimation();
        }

        while (_buttons.numChildren > 0) {
            _buttons.removeChildAt(0);
        }

        _boxClip.gotoAndPlay(1, SCN_TEXTBOX_GROW);

        replaceTimer(function () :void {
            _fadeIn.startAnimation();
            _foreground.visible = true;
        }, 400);

        _textField.htmlText = text;

        _rightButtonEdge = _textField.width;
        _leftButtonEdge = 0;

        scaleBackdrop(figureWidth(), figureHeight());

        _buttons.y = _textField.y + _textField.height;

        _foreground.visible = false;
        _backdrop.visible = this.visible = true;

        sizeChanged();

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

        scaleBackdrop(-1, figureHeight());

        sizeChanged();

        return button;
    }

    protected function clearTimer () :void
    {
        if (_timer) {
            clearTimeout(_timer);
        }
    }

    protected function replaceTimer (fun :Function, delay :Number) :void
    {
        clearTimer();
        _timer = setTimeout(function () :void { _timer = 0; fun(); }, delay);
    }

    protected function handleTextboxLoaded (evt :Event) :void
    {
        // create the textbox clip
        _boxClip = MovieClip(EmbeddedSwfLoader(evt.target).getContent());
        _boxClip.x = -Content.BOX_OFFSET.x;
        _boxClip.y = -Content.BOX_OFFSET.y;

        // just move the playhead to a 'full textbox' position and stop there
        _boxClip.gotoAndStop(1, SCN_TEXTBOX_SHRINK);

        // so we can measure it
        _boxWidth = _boxClip.width;
        _boxHeight = _boxClip.height;

        _backdrop = new Sprite();
        _backdrop.addChild(_boxClip);

        this.addChild(_backdrop);

        _foreground = new Sprite();

        _textField.x = _backdrop.x + Content.BOX_PADDING;
        _textField.y = _backdrop.y + Content.BOX_PADDING;
        _foreground.addChild(_textField);

        _buttons = new Sprite();
        _buttons.x = _textField.x;
        _foreground.addChild(_buttons);

        this.addChild(_foreground);

        _fadeIn = new AlphaFade(_foreground, 0, 1, 300);
        _fadeOut = new AlphaFade(_foreground, 1, 0, 300, function () :void {
            _foreground.visible = false;
            _boxClip.gotoAndPlay(1, SCN_TEXTBOX_SHRINK);
        });

        _done();
    }

    protected function figureWidth () :Number
    {
        return _textField.width + 2*Content.BOX_PADDING;
    }

    protected function figureHeight () :Number
    {
        return _textField.height + _buttons.height + Content.BOX_HAT + 2*Content.BOX_PADDING;
    }

    protected function scaleBackdrop (x :Number, y :Number) :void
    {
        if (x >= 0) {
            _backdrop.scaleX = x / _boxWidth;
        }
        if (y >= 0) {
            _backdrop.scaleY = y / _boxHeight;
        }
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
    protected var _boxClip :MovieClip;
    protected var _boxWidth :Number;
    protected var _boxHeight :Number;

    protected var _backdrop :Sprite;
    protected var _foreground :Sprite;
    protected var _textField :TextField;

    protected var _buttons :Sprite;
    protected var _leftButtonEdge :int;
    protected var _rightButtonEdge :int;

    protected var _timer :int;
    protected var _fadeOut :AlphaFade;
    protected var _fadeIn :AlphaFade;

    protected static const log :Log = Log.getLog(TextBox);

    protected static const SCN_TEXTBOX_GROW :String = "textbox_grow";
    protected static const SCN_TEXTBOX_SHRINK :String = "textbox_shrink";
}
}
