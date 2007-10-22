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
    public static const SWIRL_INTRO :int = 3;
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
    public function init (stageBounds :Rectangle, roomBounds :Rectangle,
                          width :Number, height :Number) :void
    {
        _stageBounds = stageBounds;
        _roomBounds = roomBounds;
        _ourWidth = width;
        _ourHeight = height;

//        this.graphics.lineStyle(4, 0xFF0000, 1.0, true, "normal", "none", "round");
//        this.graphics.drawRect(0, 0, _stageBounds.width, _stageBounds.height);
//        this.graphics.lineStyle(4, 0xFFFF00, 1.0, true, "normal", "none", "round");
//        this.graphics.drawRect(4, 4, _roomBounds.width - 8, _roomBounds.height - 8);

        // don't add the text field until the swirly is loaded
        maybeFinishUI();
    }

    protected function handleSwirlLoaded (evt :Event) :void
    {
        _swirl = MovieClip(EmbeddedSwfLoader(evt.target).getContent());
        _swirlHandler = new ClipHandler(_swirl);
        _swirl.visible = false;

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
        if (_ourWidth && _swirl && _textBox) {
            _swirl.visible = true;
            _swirl.addEventListener(MouseEvent.CLICK, swirlClicked);
            this.addChild(_swirl);

            this.addChild(_textBox);

            _swirl.x = -275; _swirl.y = -225;
            _swirl.x += 50; _swirl.y += 50;

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
        if (state == _swirlRequest) {
            log.warning("Already going to request scene [state=" + state + "]");
        } else if (state == _swirlState) {
            log.warning("Already in requested scene [state=" + state + "]");
        } else {
            _swirlRequest = state;
            maybeTransition();
        }
    }

    public function unload () :void
    {
        if (_swirlHandler) {
            _swirlHandler.unload();
        }
    }

    protected function maybeTransition () :void
    {
        if (!(_swirl && _swirl.visible && _swirlRequest)) {
            return;
        }
        var first :String = null;
        var then :String = null;

        switch(_swirlRequest) {
        case SWIRL_INTRO:
            if (_swirlState != SWIRL_NONE) {
                log.warning("Unexpected transtion [from=" + _swirlState + ", to=" +
                            _swirlRequest + "]");
            }
            // should never need a two-phase transition
            then = SCN_APPEAR;
            break;
        case SWIRL_DEMURE:
            if (_swirlState == SWIRL_NONE) {
                log.warning("Unexpected transtion [from=" + _swirlState + ", to=" +
                            _swirlRequest + "]");
                first = SCN_APPEAR;
            }
            then = SCN_MINIMIZE;
            break;
        case SWIRL_BOUNCY:
            if (_swirlState == SWIRL_DEMURE) {
//                first = SCN_MAXIMIZE;
            }
            then = SCN_LOOKATME;
            break;
        default:
            log.warning("Can't goto unknown swirl state [state=" + _swirlRequest + "]");
            return;
        }

        // there is always a 'then' transition, so write code to handle it
        var transition :Function = function () :void {
            _swirlHandler.gotoScene(then, null);
        };

        // then execute that code either as a second-phase callback, or immediately
        if (first) {
            _swirlHandler.gotoScene(first, transition);
        } else {
            transition();
        }

        _swirlState = _swirlRequest;
        _swirlRequest = 0;
    }

    protected var _tutorial :Tutorial;

    protected var _ourWidth :Number;
    protected var _ourHeight :Number;
    protected var _stageBounds :Rectangle;
    protected var _roomBounds :Rectangle;

    protected var _swirl :MovieClip;
    protected var _swirlState :int;
    protected var _swirlRequest :int;
    protected var _swirlHandler :ClipHandler;

    protected var _textBox :MovieClip;
    protected var _textField :TextField;

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

    protected static const SCN_APPEAR :String = "appear_text";
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
