package {

import flash.display.Sprite;

import flash.events.MouseEvent;

public class Piece extends Sprite
{
    public static const SIZE :int = 20;

    public static const COLORS :Array = [ 0xFFFFFF, 0x000000 ];

    public function Piece (reversi :Reversi, boardIndex :int)
    {
        _reversi = reversi;
        _boardIndex = boardIndex;

        buttonMode = true;
        addEventListener(MouseEvent.CLICK, mouseClick)
        setDisplay(Board.NO_PIECE);
    }

    public function setDisplay (
        pieceType :int, possibleMove :Boolean = false,
        myTurn :Boolean = false) :void
    {
        graphics.clear();

        if (pieceType != Board.NO_PIECE) {
            graphics.beginFill(uint(COLORS[pieceType]));
            graphics.drawCircle(SIZE/2, SIZE/2, SIZE/2);
        }

        alpha = possibleMove ? .5 : 1;
        mouseEnabled = possibleMove && myTurn;
    }

    public function showLast (lastMoved :Boolean) :void
    {
        if (lastMoved) {
            graphics.beginFill(uint(0x33FF99));
            graphics.drawCircle(SIZE/2, SIZE/2, SIZE/5);
        }
    }

    protected function mouseClick (event :MouseEvent) :void
    {
        _reversi.pieceClicked(_boardIndex);
    }

    protected var _reversi :Reversi;

    protected var _boardIndex :int; 
}
}
