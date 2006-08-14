package {

import com.threerings.util.Hashtable;

import flash.display.Sprite;
import flash.display.MovieClip;

import com.metasoy.game.Game;
import com.metasoy.game.GameObject;
import com.metasoy.game.PropertyChangedEvent;

[SWF(width="400", height="400")]
public class Reversi extends Sprite
    implements Game
{
    public function Reversi ()
    {
        _board = new Board(BOARD_SIZE);
        for (var xx :int = 0; xx < BOARD_SIZE; xx++) {
            for (var yy :int = 0; yy < BOARD_SIZE; yy++) {
                var p :Point = new Point(xx, yy);
                var piece :Piece = new Piece(this, p);
                _pieces.put(p, piece);
                addChild(piece);
            }
        }

        var max :int = BOARD_SIZE * Piece.SIZE;
//        width = height = max;

        // draw the board
        graphics.clear();
        graphics.beginFill(0x77FF77);
        graphics.drawRect(0, 0, max, max);
        graphics.endFill();

        graphics.lineStyle(1.2);
        for (var ii :int = 0; ii <= BOARD_SIZE; ii++) {
            var d :int = (ii * Piece.SIZE);
            graphics.moveTo(0, d);
            graphics.lineTo(max, d);

            graphics.moveTo(d, 0);
            graphics.lineTo(d, max);
        }

        readBoard();
        showMoves();
    }

    // from Game
    public function setGameObject (gameObj :GameObject) :void
    {
        if (_gameObject != null) {
            return; // we already got one!
        }

        _gameObject = gameObj;
        _gameObject.addEventListener(PropertyChangedEvent.TYPE, propChanged);
    }

    public function pieceClicked (p :Point) :void
    {
        _board.playPiece(p.x, p.y, _turn);
        readBoard();
        _turn = (1 - _turn);
        _gameObject.data["lastMove"] = p;
        showMoves();
    }

    protected function readBoard () :void
    {
        for (var xx :int = 0; xx < BOARD_SIZE; xx++) {
            for (var yy :int = 0; yy < BOARD_SIZE; yy++) {
                (_pieces.get(new Point(xx, yy)) as Piece).setDisplay(
                    _board.getPiece(xx, yy));
            }
        }
    }

    protected function showMoves () :void
    {
        var moves :Array = _board.getMoves(_turn);
        for each (var p :Point in moves) {
            (_pieces.get(p) as Piece).setDisplay(_turn, true);
        }
    }

    protected function propChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        if (name == "lastMove") {
            if (event.oldValue != null) {
                (_pieces.get(objToPoint(event.oldValue)) as Piece).
                    showLast(false);
            }
            (_pieces.get(objToPoint(event.newValue)) as Piece).showLast(true);
        }
    }

    protected function objToPoint (obj :Object) :Point
    {
        //return new Point(Number(obj.x), Number(obj.y));
        return (obj as Point);
    }

    protected var _pieces :Hashtable = new Hashtable();

    protected var _turn :int = Board.BLACK_IDX;

    protected static const BOARD_SIZE :int = 8;

    protected var _board :Board;

    /** Our game object. */
    protected var _gameObject :GameObject;
}
}
