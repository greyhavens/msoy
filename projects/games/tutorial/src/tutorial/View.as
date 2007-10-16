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

public class View extends Sprite
{
    public static var log :Log = Log.getLog(View);

    /** The size of the square we reserve for the swirly to the left. */
    public static const SWIRL_SIZE :int = 90;

    public function View ()
    {
        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        loader.addEventListener(Event.COMPLETE, handleSwirlLoaded);
        loader.load(ByteArray(new SWIRL()));
    }

    /**
     * Called when there's a new batch of text to display.
     */
    public function setSummary (summary :String) :void
    {
        _summary = summary;
        updateSummary();
    }

    /**
     * Called when we know our dimensions and can set up the text field.
     */
    public function init (width :Number, height :Number) :void
    {
        var format :TextFormat = new TextFormat();
        format.font = "Arial";
        format.size = 14;
        format.color = 0xDD7700;

        _textField = new TextField();
        _textField.border = false;
        _textField.borderColor = 0xFFFFFF;
        _textField.defaultTextFormat = format;
        _textField.multiline = true;
        _textField.embedFonts = false;
        _textField.autoSize = TextFieldAutoSize.NONE;
        _textField.wordWrap = true;
        _textField.width = width - SWIRL_SIZE;
        _textField.height = height;

        // don't add the text field until the swirly is loaded
        maybeFinishUI();
    }

    protected function handleSwirlLoaded (evt :Event) :void
    {
        _clip = MovieClip(EmbeddedSwfLoader(evt.target).getContent());
        _clip.addEventListener(MouseEvent.CLICK, swirlClicked);

        // don't add the swirly until the text field is loaded
        maybeFinishUI();
    }

    protected function maybeFinishUI () :void
    {
        // if both initializations are complete, actually add the bits
        if (_clip && _textField) {
            _clip.scaleX = 0.2;
            _clip.scaleY = 0.2;
            _clip.x = -10;
            _clip.y = 0;
            this.addChild(_clip);

            var square :Sprite = new Sprite();
            square.graphics.beginFill(0xFF0000);
            square.graphics.drawRect(0, 0, SWIRL_SIZE, SWIRL_SIZE);
            this.addChild(square);
            _clip.mask = square;
            _clip.gotoAndPlay(1, SCN_IDLE);

            this.addChild(_textField);
            _textField.x = SWIRL_SIZE;
            updateSummary();
        }
    }

    // only try to display our summary text when the textfield is setup
    protected function updateSummary () :void
    {
        if (_textField) {
            _textField.htmlText = _summary ? _summary : "";
        }
    }

    // some day clicking on the swirly will do something
    protected function swirlClicked (evt :Event) :void
    {
    }

    protected var _summary :String;
    protected var _textField :TextField;
    protected var _clip :MovieClip;

    [Embed(source="../../../../../rsrc/media/whatsnext.swf",
           mimeType="application/octet-stream")]
    protected static const SWIRL :Class;

    protected static const SCN_APPEAR :String = "appear";
    protected static const SCN_MAXIMIZE :String = "maximize";
    protected static const SCN_MINIMIZE :String = "minimize";
    protected static const SCN_IDLE :String = "idle";
    protected static const SCN_LOOKATME :String = "lookatme";
    protected static const SCN_GOODJOB :String = "goodjob";
    protected static const SCN_TEXTBOX :String = " textbox ";
}
}
