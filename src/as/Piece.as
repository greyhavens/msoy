package {

import flash.geom.Point;

import flash.display.Sprite;

import flash.events.MouseEvent;

public class Piece extends Sprite
{
    public static const SIZE :int = 20;

    public function Piece (reversi :Reversi, coordinate :Point)
    {
        _reversi = reversi;
        _coord = coordinate;
        height = width = SIZE;
        x = SIZE * _coord.x;
        y = SIZE * _coord.y;

        buttonMode = true;
        addEventListener(MouseEvent.CLICK, mouseClick)
        setDisplay(Board.NO_PIECE);
    }

    public function setDisplay (
            pieceType :int, possibleMove :Boolean = false) :void
    {
        graphics.clear();

        if (pieceType != Board.NO_PIECE) {
            graphics.beginFill(uint(COLORS[pieceType]));
            graphics.beginFill(0xFF00FF);
            graphics.drawCircle(SIZE/2, SIZE/2, SIZE/2);
        }

        alpha = possibleMove ? .5 : 1;
        mouseEnabled = possibleMove;
    }

    protected function mouseClick (event :MouseEvent) :void
    {
        event.stopImmediatePropagation();
        _reversi.pieceClicked(_coord);
    }

    protected var _reversi :Reversi;

    protected var _coord :Point; 

    protected static const COLORS :Array = [ 0xFFFFFF, 0x000000 ];
}
}
