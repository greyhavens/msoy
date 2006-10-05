package {

import flash.display.Sprite;

import flash.events.MouseEvent;

import mx.core.MovieClipAsset;

public class Piece extends Sprite
{
    public static const SIZE :int = 32;

    public static const LETTERS :Array = [ 0x00FFFF, 0xFF0000 ];

    public static const NO_LETTER :int = -1;

    public function Piece (alphabout :AlphaBout, piecesIndex :int, 
                           letterIndex :int)
    {
        _alphabout = alphabout;
        _piecesIndex = piecesIndex;
        _letterIndex = letterIndex;
        _letterMovie = MovieClipAsset(new _letterAnim());

        // TODO do we need both of these?
        buttonMode = true;
        mouseEnabled = true;
        _letterMovie.addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);
        _letterMovie.addEventListener(MouseEvent.MOUSE_UP, mouseReleased);

        _letterMovie.gotoAndStop(letterIndex);
        addChild(_letterMovie);
    }

    public function getLetterIndex () :int
    {
        return _letterIndex;
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
        var possibleX :int = int(x / SIZE) * SIZE + (SIZE / 2);
        var possibleY :int = int(y / SIZE) * SIZE + (SIZE / 2);
        var newIndex :int = _alphabout.letterMoved(
            _piecesIndex, possibleX, possibleY);
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

    // our source swf with the animation used for our letters
    [Embed(source="rsrc/letters/alphabet32_basic.swf#mc_BasicAlphabet")]
    protected var _letterAnim :Class
    protected var _letterMovie :MovieClipAsset;
}
}
