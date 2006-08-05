package {

import com.threerings.util.HashMap;

import flash.display.Sprite;
import flash.display.MovieClip;

import flash.external.ExternalInterface;

import flash.geom.Point;

[SWF(width="400", height="400")]
public class Reversi extends Sprite
{
    public function Reversi ()
    {
        ExternalInterface.call("console.debug",
            "this is my reversi board, stage=[" + stage + "]");

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
        graphics.beginFill(0x777777);
        graphics.drawRect(0, 0, max, max);
        graphics.endFill();

        graphics.lineStyle(2);
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

    public function pieceClicked (p :Point) :void
    {
        _board.playPiece(p.x, p.y, _turn);
        readBoard();
        _turn = (1 - _turn);
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

    protected var _pieces :HashMap = new HashMap();

    protected var _turn :int = Board.BLACK_IDX;

    protected static const BOARD_SIZE :int = 8;

    protected var _board :Board;
}
}
