package {

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
    }

    // from Game
    public function setGameObject (gameObj :GameObject) :void
    {
        if (_gameObject != null) {
            return; // we already got one!
        }

        _gameObject = gameObj;
        _gameObject.addEventListener(PropertyChangedEvent.TYPE, propChanged);

        // configure the board
        _board = new Board(_gameObject, BOARD_SIZE);

        var max :int = BOARD_SIZE * BOARD_SIZE;
        var ii :int;
        for (ii = 0; ii < max; ii++) {
            var piece :Piece = new Piece(this, ii);
            piece.x = Piece.SIZE * _board.idxToX(ii);
            piece.y = Piece.SIZE * _board.idxToY(ii);
            addChild(piece);
            _pieces[ii] = piece;
        }

//        width = height = max;

        // draw the board
        graphics.clear();
        graphics.beginFill(0x77FF77);
        graphics.drawRect(0, 0, max, max);
        graphics.endFill();

        graphics.lineStyle(1.2);
        for (ii = 0; ii <= BOARD_SIZE; ii++) {
            var d :int = (ii * Piece.SIZE);
            graphics.moveTo(0, d);
            graphics.lineTo(max, d);

            graphics.moveTo(d, 0);
            graphics.lineTo(d, max);
        }

        readBoard();
        showMoves();
    }

    public function pieceClicked (index :int) :void
    {
        _board.playPiece(index, _turn);
        readBoard();
        _turn = (1 - _turn);

        //_gameObject.set("lastMove", p);

        showMoves();
    }

    protected function readBoard () :void
    {
        for (var ii :int = 0; ii < _pieces.length; ii++) {
            (_pieces[ii] as Piece).setDisplay(_board.getPiece(ii));
        }
    }

    protected function showMoves () :void
    {
        var moves :Array = _board.getMoves(_turn);
        for each (var index :int in moves) {
            (_pieces[index] as Piece).setDisplay(_turn, true);
        }
    }

    protected function propChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        trace("property changed: " + event);
    }

    protected var _pieces :Array = new Array();

    protected var _turn :int = Board.BLACK_IDX;

    protected static const BOARD_SIZE :int = 8;

    protected var _board :Board;

    /** Our game object. */
    protected var _gameObject :GameObject;
}
}
