package {

import flash.events.TimerEvent;

import flash.display.MovieClip;

import flash.utils.Timer;

public class Explode extends BaseSprite
{
    public function Explode (xx :int, yy :int, board :Board)
    {
        super(board);
        _x = xx;
        _y = yy;
        updateLocation();

        var splode :MovieClip = MovieClip(new EXPLOSION());
        splode.x = -1.5 * SeaDisplay.TILE_SIZE;
        splode.y = -1.25 * SeaDisplay.TILE_SIZE;
        addChild(splode);

        var t :Timer = new Timer(DURATION, 1);
        t.addEventListener(TimerEvent.TIMER, remove);
        t.start();
    }

    protected function remove (evt :TimerEvent) :void
    {
        parent.removeChild(this);
    }

    protected static const DURATION :int = 800;

    [Embed(source="explosion.swf")]
    protected static const EXPLOSION :Class;
}
}
