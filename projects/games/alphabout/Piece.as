package {

import flash.display.Sprite;
import flash.geom.ColorTransform;
import flash.events.MouseEvent;

import mx.core.MovieClipAsset;

public class Piece extends Sprite
{
    public static const SIZE :int = 32;

    public static const LETTERS :Array = [ 0x00FFFF, 0xFF0000 ];

    public static const NO_LETTER :int = -1;

    public static const ASCII_OFFSET :int = 96;

    public function Piece (alphabout :AlphaBout, piecesIndex :int, 
                           letterIndex :int)
    {
        _alphabout = alphabout;
        _piecesIndex = piecesIndex;
        _letterIndex = letterIndex;
        if (letterIndex != NO_LETTER) {
            updateTheme();
        }
    }

    public function getLetterIndex () :int
    {
        return _letterIndex;
    }

    // returns the string character for this letter
    public function getLetter () :String
    {
        // TODO what should we return if NO_LETTER? Maybe just NO_LETTER
        if (hasNoLetter()) {
            return " ";
        }
        return String.fromCharCode(_letterIndex + ASCII_OFFSET);
    }

    public function hasNoLetter () :Boolean
    {
        return _letterIndex == NO_LETTER;
    }

    public function setLetterIndex (letterIndex :int) :void
    {
        _letterIndex = letterIndex;
    }

    public function setLetterInvalid () :void {
        // TODO play around with this and use constants
        this.transform.colorTransform = new ColorTransform(1, .5, .5);
    }

    public function setLetterValid () :void {
        this.transform.colorTransform = new ColorTransform(1, 1, 1);
    }

    public function updateTheme () :void
    {
        if (_letterMovie) {
            removeChild(_letterMovie);
        }
        _letterMovie = MovieClipAsset(new _themeMap[_alphabout.getTheme()]);

        // TODO do we need both of these?
        buttonMode = true;
        mouseEnabled = true;
        _letterMovie.addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);
        _letterMovie.addEventListener(MouseEvent.MOUSE_UP, mouseReleased);

        _letterMovie.gotoAndStop(_letterIndex);
        addChild(_letterMovie);
    }

    protected function mouseDown (event :MouseEvent) :void
    {
        alpha = .5;
        _previousX = x;
        _previousY = y;
        _alphabout.removeChild(this);
        _alphabout.addChild(this);
        startDrag();
    }

    protected function mouseReleased (event :MouseEvent) :void
    {
        alpha = 1;
        // TODO why are we having to fudge these numbers?
        var possibleX :int = int(x / SIZE) * SIZE + 2;
        var possibleY :int = int(y / SIZE) * SIZE + 2;
        var newIndex :int = _alphabout.letterMoved(
            _piecesIndex, possibleX, possibleY);
        // TODO consider having the piece place in the nearest spot
        // as you drag it along so when you release it goes to last
        // valid spot?
        if (newIndex != -1) {
            x = possibleX;
            y = possibleY;
            _piecesIndex = newIndex;
        } else {
            x = _previousX;
            y = _previousY;
        }
        stopDrag();
    }

    protected var _alphabout :AlphaBout;

    // where this piece is in the _pieces array. Still needed?
    protected var _piecesIndex :int;

    protected var _letterIndex :int;

    protected var _previousX :int;
    protected var _previousY :int;

    // the source for the basic letters
    [Embed(source="rsrc/letters/alphabet32_basic.swf#mc_BasicAlphabet")]
    protected static const _basicAnim :Class;

    // the source for the times letters
    [Embed(source="rsrc/letters/alphabet32_times.swf#mc_timesalphabet")]
    protected static const _timesAnim :Class;

    // the source for the ransom letters
    [Embed(source="rsrc/letters/alphabet38_ransom.swf#mc_ransomalphabet")]
    protected static const _ransomAnim :Class;

    // the source for the photo letters
    [Embed(source="rsrc/letters/alphabet32_photo.swf#mc_photoalphabet")]
    protected static const _photoAnim :Class;

    // map letter animations to theme constants from AlphaBout.as
    // TODO this could probably be done in a better way
    protected var _themeMap :Array = new Array(
        _basicAnim, _timesAnim, _ransomAnim, _photoAnim);

    protected var _letterMovie :MovieClipAsset;
}
}
