package {

import flash.display.DisplayObject;
import flash.display.Shape;
import flash.text.TextField;

import com.threerings.ezgame.PlayersDisplay;

public class AlphaBoutPlayersDisplay extends PlayersDisplay
{
    override protected function createHeader () :TextField
    {
        return null; // no header
    }

    override protected function createPlayerIcon (
        idx :int, name :String) :DisplayObject
    {
        var icon :Shape = new Shape();
        icon.graphics.beginFill(uint(Piece.LETTERS[idx]));
        icon.graphics.drawCircle(5, 5, 5);
        icon.graphics.endFill();

        icon.graphics.lineStyle(1, 0x0000FF);
        icon.graphics.drawCircle(5, 5, 5);
        return icon;
    }
}
}
