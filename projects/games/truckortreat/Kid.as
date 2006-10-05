package {

import flash.display.Sprite;

public class Kid extends Sprite
{
    
    public function Kid (startX :int, startY :int)
    {
        _health = STARTING_HEALTH;
        _x = startX;
        _y = startY;
        update();
    }
    
    public function isDead () :Boolean
    {
        if (_health <= 0) {
            return true;
        } else {
            return false;
        }
    }
    
    public function move (deltaX :int, deltaY :int) :void
    {
        var newX :int = _x + deltaX;
        var newY :int = _y + deltaY;
        if (_x != newX && 0 <= newX && newX < Board.WIDTH) {
            _x = newX;
        }
        if (_y != newY && 0 <= newY && newY < Board.HEIGHT) {
            _y = newY;
        }
        update();
    }
    
    /**
     * Update location coordinates and draw the kid on the board there.
     */
    protected function update () :void
    {
        x = _x * Board.CELL_SIZE;
        y = _y * Board.CELL_SIZE;
        // Draw a boring blue circle for the kid until we have actual art
        graphics.beginFill(0x0000FF)
        graphics.drawCircle(Board.CELL_SIZE / 2, Board.CELL_SIZE / 2, 
            Board.CELL_SIZE / 2);
    }

    /** Our location cell coordinates. */
    protected var _x :int;
    protected var _y :int;

    /** Kid's health level. */
    protected var _health :int;
    
    /** Initial health level. */
    protected static const STARTING_HEALTH :int = 3;
}
}
