package {

import flash.events.Event;

import flash.display.Shape;
import flash.display.Sprite;

import flash.geom.Matrix;

import flash.utils.getTimer; // function import

import com.threerings.flash.FrameSprite;

[SWF(width="500", height="500")]
public class FallBalls extends FrameSprite
{
    public function FallBalls ()
    {
        // Verified: a mask is enough to "reserve our shape"
        var mask :Shape = new Shape();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, 500, 500);
        mask.graphics.endFill();
        this.mask = mask;
        addChild(mask); // fucking freaky ass freakazoids
    }

    override protected function handleFrame (... ignored) :void
    {
        var now :Number = getTimer();

        addBall(now);

        for (var ii :int = _balls.length - 1; ii >= 0; ii--) {
            var ball :Ball = _balls[ii] as Ball;
            if (!ball.update(now)) {
                removeChild(ball);
                _balls.splice(ii, 1); // remove that ball, it's done.
            }
        }
    }

    protected function addBall (now :Number) :void
    {
        var matrix :Matrix = this.transform.concatenatedMatrix;
        var radius :Number = 10 + (15 * Math.random());

        var ball :Ball = new Ball(radius / matrix.a, radius / matrix.d,
            .01 + (Math.random() / 5), now);
        ball.y = -25;
        ball.x = Math.random() * 500;
        _balls.push(ball);
        addChild(ball);
    }

    protected var _balls :Array = [];

    public static const TERMINAL_Y :int = 500;
}
}

import flash.display.BlendMode;
import flash.display.Graphics;
import flash.display.Shape;

class Ball extends Shape
{
    public function Ball (xradius :Number, yradius :Number, speed :Number, now :Number) :void
    {
        blendMode = BlendMode.INVERT;
        graphics.beginFill(0xFFFFFF);
        graphics.drawEllipse(0, 0, xradius, yradius);
        graphics.endFill();

        _speed = speed;
        _lastStamp = now;
    }

    public function update (stamp :Number) :Boolean
    {
        y += (stamp - _lastStamp) * _speed;
        _lastStamp = stamp;
        return (y < FallBalls.TERMINAL_Y);
    }

    protected var _speed :Number;

    protected var _lastStamp :Number;
}
