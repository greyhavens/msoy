package {

import flash.display.Scene;
import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.geom.Point;

import mx.core.MovieClipAsset;
import mx.utils.ObjectUtil;

/**
 * A croquet ball.
 */
public class Ball extends Sprite
{
    // The radius of a ball
    public static const RADIUS :int = 13;

    // The particle representing this ball
    public var particle :BallParticle;

    public function Ball (particle :BallParticle, color :int = 1)
    {
        this.particle = particle;
        _flamingo = null;


        _ballAnimation = MovieClipAsset(new BallAnimations[color]);

        _playing = true;

        addChild(_ballAnimation);

        addEventListener(MouseEvent.CLICK, mouseClick);
    }

    /**
     * Tell the ball to stop its spinning animation.
     */
    public function stop () :void
    {
        if (_playing) {
            _playing = false;
            _ballAnimation.stop();
        }
    }

    /**
     * Tell the ball to start its spinning animation.
     */
    public function play () :void
    {
        if (!_playing) {
            _playing = true;
            _ballAnimation.play();
        }
    }

    /**
     * Hit the ball, with the cursor pulled back to the specified point.
     */
    public function hitBall (p :Point) :void
    {
        if (_flamingo == null) {
            // Hit without a flamingo? Wacky
            return;
        }

        removeChild(_flamingo);
        _flamingo = null;

        particle.addHitForce(p);
    }

    /**
     * Mouse click handler, adds/removes a flamingo.
     */
    protected function mouseClick (event :MouseEvent) :void
    {
        if (_flamingo != null) {
            removeChild(_flamingo);
            _flamingo = null;
            return;
        }
        _flamingo = new WonderlandFlamingo(this);
        addChild(_flamingo);
    }

    // Our flamingo we're using for aiming.
    protected var _flamingo :WonderlandFlamingo;

    // The player that controls this ball.
    protected var _playerId :int;

    // Modifiers applied to this ball.
    protected var _modifiers :int;

    // Our actual artwork
    protected var _ballAnimation :MovieClipAsset;

    // Are we currently playing?
    protected var _playing :Boolean;

    // Modifier options.
    protected static const MODIFIER_GIANT    :int = 0x1 << 0;
    protected static const MODIFIER_TINY     :int = 0x1 << 1;
    protected static const MODIFIER_PAINTED  :int = 0x1 << 2;


    // The ball artwork.
    [Embed(source="rsrc/ball.swf#ball1")]
    protected static var ballAnimation1Class :Class;
    [Embed(source="rsrc/ball.swf#ball2")]
    protected static var ballAnimation2Class :Class;
    [Embed(source="rsrc/ball.swf#ball3")]
    protected static var ballAnimation3Class :Class;
    [Embed(source="rsrc/ball.swf#ball4")]
    protected static var ballAnimation4Class :Class;
    [Embed(source="rsrc/ball.swf#ball5")]
    protected static var ballAnimation5Class :Class;
    [Embed(source="rsrc/ball.swf#ball6")]
    protected static var ballAnimation6Class :Class;

    protected static var BallAnimations :Array = [
        ballAnimation1Class,
        ballAnimation2Class,
        ballAnimation3Class,
        ballAnimation4Class,
        ballAnimation5Class,
        ballAnimation6Class,
    ];
}

}
