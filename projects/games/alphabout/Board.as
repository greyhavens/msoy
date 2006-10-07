package {

import flash.display.Graphics;
import flash.display.Sprite;
import mx.core.BitmapAsset;

public class Board extends Sprite
{
    public function Board (alphabout :AlphaBout, size :int = 15)
    {
        _alphabout = alphabout;
        _size = size;
        changeTheme();
    }

    public function getBorder () :int
    {
        return _border;
    }

    public function getSize () :int
    {
        return _size;
    }

    public function changeTheme () :void
    {
        if (_boardBitmap != null) {
            removeChild(_boardBitmap);
        }
        switch (_alphabout.getTheme()) {
          case AlphaBout.BASIC_THEME:
            _boardBitmap = new _basicBoard();
            break;
          case AlphaBout.TIMES_THEME:
            _boardBitmap = new _timesBoard();
            break;
          case AlphaBout.RANSOM_THEME:
            _boardBitmap = new _ransomBoard();
            break;
          case AlphaBout.PHOTO_THEME:
            _boardBitmap = new _photoBoard();
            break;
        }
        addChild(_boardBitmap);
    }

    protected var _boardBitmap :BitmapAsset = null;

    // our board for the basic theme
    [Embed(source="rsrc/boards/basic.jpg")]
    protected static const _basicBoard :Class;

    // our board for the photo theme
    [Embed(source="rsrc/boards/photo.jpg")]
    protected static const _photoBoard :Class;

    // our board for the times theme
    [Embed(source="rsrc/boards/times.jpg")]
    protected static const _timesBoard :Class;

    // our board for the ransom theme
    [Embed(source="rsrc/boards/ransom.jpg")]
    protected static const _ransomBoard :Class;

    protected var _alphabout :AlphaBout;
    protected var _size :int;
    protected var _border :int = 50;
}
}
