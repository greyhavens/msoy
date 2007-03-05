package {

import flash.display.MovieClip;
import flash.display.Sprite;

import flash.events.Event; 
import flash.events.MouseEvent; 

import flash.geom.Point;

import com.threerings.util.EmbeddedSwfLoader;

public class WonderlandMallet extends Sprite
{
    public function WonderlandMallet (ball :Ball)
    {
        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        loader.addEventListener(Event.COMPLETE, function (event :Event) :void {
            _swingAnimation = new (loader.getClass("mallet_swing"))();
            _chargeAnimation = new (loader.getClass("mallet_charge"))();

            _swingAnimation.y = -2 * Ball.RADIUS;
            _chargeAnimation.y = -2 * Ball.RADIUS;
            addChild(_chargeAnimation);

            // And now start in a reasonablish place.
            _chargeAnimation.gotoAndStop(Math.round(CHARGE_FRAMES / 2));

            _chargeAnimation.addEventListener(MouseEvent.MOUSE_DOWN, mouseDown);
        });

        loader.load(new MALLET_ART_CLASS());

        _ball = ball;
    }

    protected function mouseDown (event :MouseEvent) :void
    {
        _ball.particle.wc.addEventListener(MouseEvent.MOUSE_MOVE, mouseMove);
        _ball.particle.wc.addEventListener(MouseEvent.MOUSE_UP, mouseUp);

        _ballPosition = _ball.parent.localToGlobal(new Point(_ball.x, _ball.y));
    }

    protected function mouseMove (event :MouseEvent) :void
    {
        var p :Point = _ball.particle.wc.globalToLocal(new Point(event.stageX, event.stageY));

        var dx :Number = _ballPosition.x - p.x;
        var dy :Number = _ballPosition.y - p.y;

        // First, rotate this sucker so it matches where we are
        var theta :Number
        if (p.y == _ballPosition.y) {
            // we're in a horizontal line with our ball.
            if (p.x > _ballPosition.x) {
                theta = 90;
            } else {
                theta = -90;
            }
            
        } else {
            // More likely than not, we need to do some trig. Math is hard; let's go shopping.

            theta = (- Math.atan((_ballPosition.x - p.x)/(_ballPosition.y - p.y))) *
                    180.0/Math.PI;

            if (p.y > _ballPosition.y) {
                theta += 180;
            }
        }

        rotation = theta;

        // Now stretch it to match our strength
        var strength :Number = Ball.computeStrength(dx, dy);

        _chargeAnimation.gotoAndStop(Math.round(CHARGE_FRAMES - 
                                     (CHARGE_FRAMES * strength/ Ball.MAX_HIT_STRENGTH)));

    }

    protected function mouseUp (event :MouseEvent) :void
    {
        _ball.particle.wc.removeEventListener(MouseEvent.MOUSE_MOVE, mouseMove);
        _ball.particle.wc.removeEventListener(MouseEvent.MOUSE_UP, mouseUp);

        _strikePoint = _ball.globalToLocal(new Point(event.stageX, event.stageY));

        _swingAnimation.gotoAndPlay(Math.round(_chargeAnimation.currentFrame / 
                                               (CHARGE_FRAMES / SWING_FRAMES)));
        removeChild(_chargeAnimation);
        addChild(_swingAnimation);

        addEventListener(Event.ENTER_FRAME, watchSwing);
    }

    protected function watchSwing (event :Event) :void
    {
        if (_swingAnimation.currentFrame >= SWING_FRAMES) {
            removeEventListener(Event.ENTER_FRAME, watchSwing);
            _ball.hitBall(_strikePoint);
            addEventListener(Event.ENTER_FRAME, finishSwing);
        }
    }

    protected function finishSwing (event :Event) :void
    {
        if ((_swingAnimation.currentFrame == (_swingAnimation.totalFrames - 1)) ||
            (_swingAnimation.currentFrame < SWING_FRAMES)) {
            removeEventListener(Event.ENTER_FRAME, finishSwing);
            _ball.destroyMallet();
        }
    }

    /** The current position of our ball in global coordinates, for convenience while dragging
     * this sucker around. */
    protected var _ballPosition :Point;

    /** The position we're striking from; used to keep it around while we play the fun little
     * animation. */
    protected var _strikePoint :Point;

    /** The animation of our mallet swinging & striking the ball. */
    protected var _swingAnimation :MovieClip;

    /** The animation to step through on our backswing while charging up our shot. */
    protected var _chargeAnimation :MovieClip;

    /** A copy of the ball we control. */
    protected var _ball :Ball;

    /** The number of frames in the charge animation. */
    protected static const CHARGE_FRAMES :int = 49;

    /** The number of frames in the swing animation that correspond to our charge animation.
      * This is used for converting between the two so we can play the fast animation from an
      * appropriate spot based on our charge. */
    protected static const SWING_FRAMES :int = 7;

    [Embed(source="rsrc/mallet.swf", mimeType="application/octet-stream")]
    protected static const MALLET_ART_CLASS :Class;
}
}
