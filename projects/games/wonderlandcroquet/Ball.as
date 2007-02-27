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

    // The index of the player that owns this ball 
    public var playerIdx :int;

    public function Ball (particle :BallParticle, playerIdx :int = 0)
    {
        this.particle = particle;
        this.playerIdx = playerIdx;
        _flamingo = null;

        _ballAnimation = MovieClipAsset(new ballAnimations[playerIdx]);

        _playing = true;
        stop();

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
            particle.velocity.magnitude() != 0) {
            // Not ours/not our turn/moving
            return;
        }

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

    // Multiplier applied to the length of the vector they've drawn for hit strength.
    protected static const HIT_STRENGTH_MULTIPLIER :int = 2;

    // The hardest anyone is allowed to hit a ball
    protected static const MAX_HIT_STRENGTH :int = 300;

    // The ball artwork.
    [Embed(source="rsrc/ball.swf#ball1")]
    protected static var BallAnimation1 :Class;
    [Embed(source="rsrc/ball.swf#ball2")]
    protected static var BallAnimation2 :Class;
    [Embed(source="rsrc/ball.swf#ball3")]
    protected static var BallAnimation3 :Class;
    [Embed(source="rsrc/ball.swf#ball4")]
    protected static var BallAnimation4 :Class;
    [Embed(source="rsrc/ball.swf#ball5")]
    protected static var BallAnimation5 :Class;
    [Embed(source="rsrc/ball.swf#ball6")]
    protected static var BallAnimation6 :Class;

    protected static var ballAnimations :Array = [
        BallAnimation1,
        BallAnimation2,
        BallAnimation3,
        BallAnimation4,
        BallAnimation5,
        BallAnimation6,
    ];
}

}
