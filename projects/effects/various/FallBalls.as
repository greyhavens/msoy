package {

import flash.events.Event;

import flash.display.Sprite;

import flash.utils.getTimer; // function import

[SWF(width="2000", height="500")]
public class FallBalls extends Sprite
{
    public function FallBalls ()
    {
        addEventListener(Event.ENTER_FRAME, handleFrame);
    }

    protected function handleFrame (... ignored) :void
    {
        var now :Number = getTimer();

        addBall(now);

        for (var ii :int = _balls.length - 1; ii >= 0; ii--) {
            if (!_balls[ii].update(now)) {
                _balls.splice(ii, 1); // remove that ball, it's done.
            }
        }
    }

    protected function addBall (now :Number) :void
    {
        var ball :Ball = new Ball(10 + (Math.random() * 15), .01 + (Math.random() / 5), now);
        ball.y = -25;
        ball.x = Math.random() * 2000;
        // start a new ball
        _balls.push(ball);
        addChild(ball);
    }

    protected var _balls :Array = [];

    public static const TERMINAL_Y :int = 600;
}
}

import flash.display.BlendMode;
import flash.display.Graphics;
import flash.display.Sprite;

class Ball extends Sprite
{
    public function Ball (radius :Number, speed :Number, now :Number) :void
    {
        blendMode = BlendMode.INVERT;
        graphics.beginFill(0xFFFFFF);
        graphics.drawCircle(0, 0, radius);
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
