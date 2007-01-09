package {

import flash.display.Scene;
import flash.display.Sprite;
import flash.events.MouseEvent;

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

    public function Ball (particle :BallParticle, color :int = 0xff0000)
    {
        this.particle = particle;

        _ballAnimation = MovieClipAsset(new ballAnimationClass());
        _playing = true;

        addChild(_ballAnimation);

        // FIXME: this shouldn't be needed
        
        addEventListener(MouseEvent.CLICK, mouseClick);
    }

    public function stop () :void
    {
        if (_playing) {
            _playing = false;
            _ballAnimation.stop();
        }
    }

    public function play () :void
    {
        if (!_playing) {
            _playing = true;
            _ballAnimation.play();
        }
    }

    protected function mouseClick (event :MouseEvent) :void
    {
        particle.addRandomForce();
    }

    protected var _dragging :Boolean;

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

    [Embed(source="rsrc/ball.swf#hedgehog")]
    protected var ballAnimationClass :Class;
}

}
