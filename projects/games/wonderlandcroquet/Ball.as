package {

import flash.display.DisplayObject;
import flash.display.Scene;
import flash.display.Sprite;
import flash.display.MovieClip;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Point;

import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFieldAutoSize;

import mx.utils.ObjectUtil;

import com.threerings.util.EmbeddedSwfLoader;

/**
 * A croquet ball.
 */
public class Ball extends Sprite
{
    // The radius of a ball
    public static const RADIUS :int = 13;

    // The hardest anyone is allowed to hit a ball
    public static const MAX_HIT_STRENGTH :int = 300;

    // The particle representing this ball
    public var particle :BallParticle;

    // The index of the player that owns this ball 
    public var playerIdx :int;

    // A marker to show when our ball is obstructed by something
    public var ballMarker :Sprite;

    public function Ball (particle :BallParticle, playerIdx :int = 0)
    {
        this.particle = particle;
        this.playerIdx = playerIdx;
        _mallet = null;
        ballMarker = new Sprite();

        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        loader.addEventListener(Event.COMPLETE, function (event :Event) :void {
            ballMarker.addChild(new (loader.getClass("ballmarker" + (playerIdx + 1)))());
            var nameText :TextField = new TextField();
            nameText.autoSize = TextFieldAutoSize.CENTER;
            nameText.selectable = false;
            nameText.x = 0;
            nameText.y = - (RADIUS * 3);

            var nameFormat :TextFormat = new TextFormat();
            nameFormat.font = "Verdana";
            nameFormat.color = BALL_COLORS[playerIdx];
            nameFormat.size = 16;
            nameFormat.bold = true;
            nameText.defaultTextFormat = nameFormat;
            nameText.text = (particle.wc.gameCtrl.seating.getPlayerNames())[playerIdx];

            ballMarker.addChild(nameText);

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
     * Figure out how strong a hit is.
     */
    public static function computeStrength (dx :Number, dy :Number) :Number
    {
        return Math.min(MAX_HIT_STRENGTH, Math.sqrt(dx*dx + dy*dy) * HIT_STRENGTH_MULTIPLIER);
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

        var angle :Number = Math.PI + Math.atan(p.y / p.x);

        var strength :Number = computeStrength(p.x, p.y);

        if (p.x < 0) {
            angle += Math.PI;
        }

        angle += rotation * Math.PI/180;
        
        particle.wc.gameCtrl.set("lastHit", 
            [playerIdx, Math.cos(angle) * strength, Math.sin(angle) * strength]);
    }

    public function destroyMallet () :void
    {
        particle.wc.removeMallet(_mallet);
        _mallet = null;
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
            particle.wc.removeMallet(_mallet);
            _mallet = null;
            return;
        }
        _mallet = new WonderlandMallet(this);
        _mallet.x = x;
        _mallet.y = y;
        _mallet.rotation = rotation + 180;
        particle.wc.addMallet(_mallet);
    }

    // Our flamingo we're using for aiming.
    protected var _mallet :WonderlandMallet;

    // Modifiers applied to this ball.
    protected var _modifiers :int;

    // Our actual artwork
    protected var _ballAnimation :MovieClip;

    // Are we currently playing?
    protected var _playing :Boolean = false;

    // The colors for our different balls
    protected static const BALL_COLORS :Array = 
        [0x0099ff, 0xcc0000, 0x473564, 0xffff00, 0x008040, 0xff9900];

    // Modifier options.
    protected static const MODIFIER_GIANT    :int = 0x1 << 0;
    protected static const MODIFIER_TINY     :int = 0x1 << 1;
    protected static const MODIFIER_PAINTED  :int = 0x1 << 2;

    // Multiplier applied to the length of the vector they've drawn for hit strength.
    protected static const HIT_STRENGTH_MULTIPLIER :int = 2;

    [Embed(source="rsrc/ball.swf", mimeType="application/octet-stream")]
    protected static const BALL_ART_CLASS :Class;
}
}
