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

public class View extends Sprite
{
    public static const SWIRL_NONE :int = 1;
    public static const SWIRL_DEMURE :int = 2;
    public static const SWIRL_HUGE :int = 3;
    public static const SWIRL_BOUNCY :int = 4;

    public function View (tutorial :Tutorial)
    {
        _tutorial = tutorial;

        var swirlBytes :ByteArray = ByteArray(new SWIRL());

        var loader :EmbeddedSwfLoader;

        // load two separate swirls, one to use as the actual swirly
        loader = new EmbeddedSwfLoader();
        loader.addEventListener(Event.COMPLETE, handleSwirlLoaded);
        loader.load(swirlBytes);

        // and one to use as the textbox
        loader = new EmbeddedSwfLoader();
        loader.addEventListener(Event.COMPLETE, handleTextboxLoaded);
        loader.load(swirlBytes);

        _swirlState = SWIRL_NONE;
    }

    /**
     * Called when we know our dimensions and can set up the text field.
     */
    public function init (width :Number, height :Number) :void
    {
        _width = width;
        _height = height;

        var format :TextFormat = new TextFormat();
        format.font = "SunnySide";
        format.size = 14;
        format.color = 0xDD7700;

        // don't add the text field until the swirly is loaded
        maybeFinishUI();
    }

    protected function handleSwirlLoaded (evt :Event) :void
    {
        _swirl = MovieClip(EmbeddedSwfLoader(evt.target).getContent());
        _swirl.visible = false;

        _todoDeleteMe = buildHugeSwirlTextLayer();

        _scenes = new Object();
        for (var ii :int = 0; ii < _swirl.scenes.length; ii ++) {
            var scene :Scene = _swirl.scenes[ii];
            _scenes[scene.name] = scene;
        }

        // don't add the swirly until the text field is loaded
        maybeFinishUI();
    }

    protected function handleTextboxLoaded (evt :Event) :void
    {
        _textBox = MovieClip(EmbeddedSwfLoader(evt.target).getContent());
        _textBox.gotoAndStop(1, SCN_TEXTBOX);
        _textBox.visible = false;
        _textBox.x = 80;
        _textBox.y = 80;

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

        log.info(styleSheet.styleNames);

        var format :TextFormat = new TextFormat();
        format.font = "SunnySide";
        format.size = 14;
        format.color = 0x000000;
        format.align = TextFormatAlign.LEFT;

        _textField = new TextField();
//        _textField.border = true;
//        _textField.borderColor = 0x000000;
        _textField.defaultTextFormat = format;
        _textField.styleSheet = styleSheet;
        _textField.wordWrap = true;
        _textField.multiline = true;
        _textField.embedFonts = true;
        _textField.antiAliasType = flash.text.AntiAliasType.ADVANCED;
        _textField.autoSize = TextFieldAutoSize.NONE;
  
        _textField.width = _textBox.width - (BOX_OFFSET_LEFT + BOX_OFFSET_RIGHT);
        _textField.height = _textBox.height - (BOX_OFFSET_TOP + BOX_OFFSET_BOTTOM);
        _textField.x = BOX_OFFSET_LEFT;
        _textField.y = BOX_OFFSET_TOP;
        _textBox.addChild(_textField);

        var button :SimpleButton = new SimpleTextButton(
            "Hide", true, 0x003366, 0x6699CC, 0x0066FF, 5, format);
        _textBox.addChild(button);
        button.addEventListener(MouseEvent.CLICK, function (evt :Event) :void {
                displaySummary(null);
            });
        button.x = 440 - button.width;
        button.y = 335 - button.height;

        // don't add the swirly until the text field is loaded
        maybeFinishUI();
    }

    protected function maybeFinishUI () :void
    {
        // if both initializations are complete, actually add the bits
        if (_width && _swirl && _textBox) {
            _swirl.visible = true;
            _swirl.addEventListener(MouseEvent.CLICK, swirlClicked);
            this.addChild(_swirl);

            _todoDeleteMe.visible = false;
            _todoDeleteMe.addEventListener(MouseEvent.CLICK, swirlClicked);
            this.addChild(_todoDeleteMe);

            this.addChild(_textBox);

            maybeTransition();
        }
    }

    public function isShowingSummary () :Boolean
    {
        return _textBox.visible;
    }

    public function displaySummary (summary :String) :void
    {
        if (summary) {
            _textBox.visible = true;
            _textField.htmlText = summary;
//            _textBox.height = _textField.height + BOX_OFFSET_TOP + BOX_OFFSET_BOTTOM;
//            _textBox.width = _textField.width + BOX_OFFSET_LEFT + BOX_OFFSET_RIGHT;
        } else {
            _textBox.visible = false;
        }
    }

    // some day clicking on the swirly will do something
    protected function swirlClicked (evt :Event) :void
    {
        // when the swirly is big, clicking it offers the first quest
        _tutorial.swirlClicked(_swirlState);
    }

    public function gotoSwirlState (state :int) :void
    {
        // TODO: If we're really going to use the big swirly a lot we absolutely must have
        // TODO: proper transitions; this is a temporary hack

        switch(state) {
        case SWIRL_HUGE:
            if (_swirlState != SWIRL_NONE) {
                log.warning("Unexpected transtion [from=" + _swirlState + ", to=" + state + "]");
            }
            _transition = SCN_APPEAR;
            break;
        case SWIRL_DEMURE:
            if (_swirlState != SWIRL_DEMURE) {
                _transition = SCN_MINIMIZE;
            }
            // TODO: if we go from NONE to DEMURE, do we SCN_APPEAR first?
            // TODO: probably not; we'd need a SMALL_APPEAR
            break;
        case SWIRL_BOUNCY:
            if (_swirlState != SWIRL_DEMURE) {
                // TODO: this will look moronic without a MAXIMIZE transition first
                log.warning("Unexpected transtion [from=" + _swirlState + ", to=" + state + "]");
            }
            _transition = SCN_LOOKATME;
            break;
        default:
            log.warning("Can't goto unknown swirl state [state=" + state + "]");
            return;
        }
        _swirlState = state;
        maybeTransition();
    }

    protected function maybeTransition () :void
    {
        if (_swirl && _swirl.visible && _transition) {
            _swirl.gotoAndPlay(1, _transition);
            // TODO: make this appear slowly, probably as part of the .swf
            _todoDeleteMe.visible = (_transition == SCN_APPEAR || _transition == SCN_MAXIMIZE);
            _transition = null;
        }
    }

    // TODO: ask Bill to simply include this in the SWF?
    protected function buildHugeSwirlTextLayer() :DisplayObject
    {
        var textLayer :Sprite = new Sprite();

        var format :TextFormat = new TextFormat();
        format.font = "SunnySide";
        format.size = 28;
        format.color = 0x203344;
        format.align = TextFormatAlign.CENTER;

        var topText :TextField = new TextField();
        topText.defaultTextFormat = format;
        topText.multiline = true;
        topText.wordWrap = true;
        topText.selectable = false;
        topText.embedFonts = true;
        topText.antiAliasType = AntiAliasType.ADVANCED;
        topText.x = 0;
        topText.y = 0;
        topText.width = 180;
        topText.height = 100;
        topText.x = 190;
        topText.y = 90;
        topText.htmlText = "Welcome to<br>Whirled!";
        textLayer.addChild(topText);

        format = new TextFormat();
        format.font = "SunnySide";
        format.size = 16;
        format.color = 0x203344;
        format.italic = true;
        format.align = TextFormatAlign.CENTER;

        var midText :TextField = new TextField();
        midText.defaultTextFormat = format;
        midText.multiline = true;
        midText.wordWrap = true;
        midText.embedFonts = true;
        midText.antiAliasType = AntiAliasType.ADVANCED;
        midText.selectable = false;
        midText.x = 0;
        midText.y = 0;
        midText.width = 240;
        midText.height = 100;
        midText.x = 160;
        midText.y = 180;
        midText.htmlText = "Whirled is a web-based social world for chat, games and player-created content.";
        textLayer.addChild(midText);

        format = new TextFormat();
        format.font = "SunnySide";
        format.size = 18;
        format.color = 0x323E44;
        format.align = TextFormatAlign.CENTER;

        var botText :TextField = new TextField();
        botText.defaultTextFormat = format;
        botText.multiline = true;
        botText.wordWrap = true;
        botText.embedFonts = true;
        botText.antiAliasType = AntiAliasType.ADVANCED;
        botText.selectable = false;
        botText.x = 0;
        botText.y = 0;
        botText.width = 180;
        botText.height = 100;
        botText.x = 190;
        botText.y = 260;
        botText.htmlText = "Click me to begin exploring Whirled!";
        textLayer.addChild(botText);

        return textLayer;
    }

    protected var _tutorial :Tutorial;

    protected var _width :Number;
    protected var _height :Number;

    protected var _swirl :MovieClip;
    protected var _swirlState :int;
    protected var _transition :String;

    protected var _textBox :MovieClip;
    protected var _textField :TextField;

    protected var _scenes :Object;

    protected var _todoDeleteMe :DisplayObject;

    protected static const log :Log = Log.getLog(View);

    [Embed(source="../../rsrc/whatsnext.swf", mimeType="application/octet-stream")]
    protected static const SWIRL :Class;

    [Embed(source="../../rsrc/SunnySide.ttf", fontName="SunnySide",
           unicodeRange="U+0020-U+007E,U+2022")]
    protected static const FONT_SUNNYSIDE :Class;

    [Embed(source="../../rsrc/Goudy.ttf", fontName="Goudy",
           unicodeRange="U+0020-U+007E,U+2022")]
    protected static const FONT_GOUDY :Class;

     [Embed(source="../../rsrc/GoudyB.ttf", fontName="Goudy", fontWeight="bold",
             unicodeRange="U+0020-U+007E,U+2022")]
    protected static const FONT_GOUDY_BOLD :Class;

    [Embed(source="../../rsrc/GoudyI.ttf", fontName="Goudy", fontStyle="italic",
            unicodeRange="U+0020-U+007E,U+2022")]
    protected static const FONT_GOUDY_ITALIC :Class;

    [Embed(source="../../rsrc/GoudyBI.ttf", fontName="Goudy", fontWeight="bold",
           fontStyle="italic", unicodeRange="U+0020-U+007E,U+2022")]
    protected static const FONT_GOUDY_BOLD_ITALIC :Class;

    protected static const SCN_APPEAR :String = "appear";
    protected static const SCN_MAXIMIZE :String = "maximize";
    protected static const SCN_MINIMIZE :String = "minimize";
    protected static const SCN_IDLE :String = "idle";
    protected static const SCN_LOOKATME :String = "lookatme";
    protected static const SCN_GOODJOB :String = "goodjob";
    protected static const SCN_TEXTBOX :String = "textbox";

    protected static const BOX_OFFSET_LEFT :int = 70;
    protected static const BOX_OFFSET_TOP :int = 80;
    protected static const BOX_OFFSET_RIGHT :int = -35;
    protected static const BOX_OFFSET_BOTTOM :int = 5;
}
}
