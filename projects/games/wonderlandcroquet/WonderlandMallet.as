package {

import flash.display.Sprite;
import flash.geom.Point;
import flash.events.MouseEvent; 

import com.threerings.util.EmbeddedSwfLoader;

public class WonderlandMallet extends Sprite
{

    public function WonderlandMallet (ball :Ball)
    {
        _ball = ball;

        _circle = new Circle();
        addChild(_circle);


        _circle.addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);
        _circle.addEventListener(MouseEvent.MOUSE_UP, mouseUp);
    }

    protected function mouseDown (event :MouseEvent) :void
    {
        _circle.startDrag(true);
        _circle.addEventListener(MouseEvent.MOUSE_MOVE, mouseMove);
    }

    protected function mouseUp (event :MouseEvent) :void
    {
        _circle.stopDrag();
        _circle.removeEventListener(MouseEvent.MOUSE_MOVE, mouseMove);

        var p :Point = _ball.globalToLocal(new Point(event.stageX, event.stageY));

        _ball.hitBall(p);
    }

    protected function mouseMove (event :MouseEvent) :void
    {
    /* Laser-sighting code disabled for now.
        var p :Point = _ball.globalToLocal(new Point(event.stageX, event.stageY));
        var slope :Number = p.y / p.x;

        var len :int = - 500;

        if (p.x < 0) {
            len *= -1;
        }

        graphics.clear();

        graphics.lineStyle(1, 0xff0000);
        graphics.moveTo(0, 0);
        graphics.lineTo(len, len * slope);
    */
    }

    protected var _circle :Sprite;

    protected var _ball :Ball;

    [Embed(source="rsrc/mallet.swf", mimeType="application/octet-stream")]
    protected static const MALLET_CLASS :Class;
}
}

import flash.display.Sprite;

class Circle extends Sprite
{
    public function Circle ()
    {
        graphics.beginFill(0xff0000);
        graphics.drawCircle(0, 0, 5);
    }
}
