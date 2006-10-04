package {

import flash.events.TimerEvent;

import flash.utils.Timer;

public class Explosion extends BaseSprite
{
    public function Explosion (xx :int, yy :int, duration :int, board :Board)
    {
        super(board);
        _x = xx;
        _y = yy;
        updateLocation();

        graphics.moveTo(Math.random() * SeaDisplay.TILE_SIZE,
            Math.random() * SeaDisplay.TILE_SIZE);
        for (var ii :int = 0; ii < 10; ii++) {
            var color :uint = ((Math.random() * 256) << 16) |
                ((Math.random() * 256) << 8) | (Math.random() * 256);
            graphics.lineStyle(1, color);
            graphics.lineTo(Math.random() * SeaDisplay.TILE_SIZE,
                Math.random() * SeaDisplay.TILE_SIZE);
        }

        var t :Timer = new Timer(duration, 1);
        t.addEventListener(TimerEvent.TIMER, remove);
        t.start();
    }

    protected function remove (evt :TimerEvent) :void
    {
        parent.removeChild(this);
    }
}
}
