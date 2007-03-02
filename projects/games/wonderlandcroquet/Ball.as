package {

import flash.display.DisplayObject;
import flash.display.Scene;
import flash.display.Sprite;
import flash.display.MovieClip;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;

import mx.utils.ObjectUtil;

import com.threerings.util.EmbeddedSwfLoader;

/**
 * A croquet ball.
 */
public class Ball extends Sprite
{
    // The radius of a ball
    public static const RADIUS :int = 13;

    // The particle representing this ball
    public var particle :BallParticle;

    // The index of the player that owns this ball 
    public var playerIdx :int;

    // A marker to show when our ball is obstructed by something
    public var ballMarker :DisplayObject;

    public function Ball (particle :BallParticle, playerIdx :int = 0)
    {
        this.particle = particle;
        this.playerIdx = playerIdx;
        _mallet = null;

        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        loader.addEventListener(Event.COMPLETE, function (event :Event) :void {
            ballMarker = new (loader.getClass("ballmarker" + (playerIdx + 1)))();
            _ballAnimation = new (loader.getClass("ball" + (playerIdx + 1)))();
            addChild(_ballAnimation);

            _playing = true;
            stop();
        });

        loader.load(new BALL_ART_CLASS());


        addEventListener(MouseEvent.CLICK, mouseClick);
    }

    /**
     * Tell the ball to stop its spinning animation.
     */
    public function stop () :void
    {
        if (_ballAnimation == null) { 
            return;
        }

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
        if (_ballAnimation == null) { 
            return;
        }

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
        if (_mallet == null) {
            // Hit without a mallet? Wacky
            return;
        }

        removeChild(_mallet);
        _mallet = null;

        var angle :Number = Math.PI + Math.atan(p.y / p.x);
        var strength :Number = Math.sqrt(p.x*p.x + p.y*p.y) * HIT_STRENGTH_MULTIPLIER;

        strength = Math.min(strength, MAX_HIT_STRENGTH);

        if (p.x < 0) {
            angle += Math.PI;
        }

        angle += rotation * Math.PI/180;
        
        particle.wc.gameCtrl.set("lastHit", 
            [playerIdx, Math.cos(angle) * strength, Math.sin(angle) * strength]);
    }

    /**
     * Mouse click handler, adds/removes a flamingo.
     */
    protected function mouseClick (event :MouseEvent) :void
    {
        if (particle.wc.gameCtrl == null || !particle.wc.gameCtrl.isMyTurn() ||
            particle.wc.myIdx != playerIdx || particle.velocity.magnitude() != 0) {
            // Not ours/not our turn/moving
            return;
        }

        if (_mallet != null) {
            removeChild(_mallet);
            _mallet = null;
            return;
        }
        _mallet = new WonderlandMallet(this);
        addChild(_mallet);
    }

    // Our flamingo we're using for aiming.
    protected var _mallet :WonderlandMallet;

    // Modifiers applied to this ball.
    protected var _modifiers :int;

    // Our actual artwork
    protected var _ballAnimation :MovieClip;

    // Are we currently playing?
    protected var _playing :Boolean = false;

    // Modifier options.
    protected static const MODIFIER_GIANT    :int = 0x1 << 0;
    protected static const MODIFIER_TINY     :int = 0x1 << 1;
    protected static const MODIFIER_PAINTED  :int = 0x1 << 2;

    // Multiplier applied to the length of the vector they've drawn for hit strength.
    protected static const HIT_STRENGTH_MULTIPLIER :int = 2;

    // The hardest anyone is allowed to hit a ball
    protected static const MAX_HIT_STRENGTH :int = 300;

    [Embed(source="rsrc/ball.swf", mimeType="application/octet-stream")]
    protected static const BALL_ART_CLASS :Class;
}

}
