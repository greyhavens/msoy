package {

import flash.display.Scene;
import flash.display.Sprite;
import flash.utils.describeType;
import mx.core.BitmapAsset;
import mx.core.MovieClipAsset;

public class Bunny extends Sprite
{
    public function Bunny ()
    {
        bunnyMovie = MovieClipAsset(new bunnyAnim());
        bunnyMovie.gotoAndStop(2);
        addChild(bunnyMovie);
        idle();
    }

    public function walk (deltaX :int) :void
    {
        x = Math.min(640 - width, Math.max(0, x + deltaX));
        if (bunnyMovie.currentFrame != 2) {
            bunnyMovie.gotoAndStop(2);
        }
        var newOrient :int;
        if (deltaX < 0) {
            newOrient = 1;
        } else {
            newOrient = 0;
        }

        if (newOrient != orient) {
            orient = newOrient;
            switch (orient) {
              case 0:
                scaleX = 1;
                break;
              case 1:
                scaleX = -1;
                break;
            }
        }
    }

    public function idle () :void
    {
        bunnyMovie.gotoAndStop(1);
    }

    protected var bunnyMovie :MovieClipAsset;

    protected var orient :int;

    [Embed(source="rsrc/bunny/bunny_knight_blue.swf#bunny_knight_blue")]
    protected var bunnyAnim :Class;
}
}
