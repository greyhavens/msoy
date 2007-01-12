package {

import flash.display.Sprite;

import flash.events.Event;

import mx.core.MovieClipAsset;

public class Explosion extends Sprite
{
    public function Explosion (x :int, y :int, rot :int, isSmall :Boolean,
        shipType :int, parent :BoardSprite)
    {
        _parent = parent;
        _isSmall = isSmall;

        if (isSmall) {
            _explodeMovie = MovieClipAsset(new smExplodeAnim());
        } else {
            _explodeMovie =
                MovieClipAsset(new Codes.SHIP_TYPES[shipType].EXPLODE_ANIM);
        }
        _explodeMovie.x = _explodeMovie.width/2;
        _explodeMovie.y = -_explodeMovie.height/2;
        _explodeMovie.rotation = 90;
        _explodeMovie.addEventListener(Event.ENTER_FRAME, endExplode);
        _explodeMovie.gotoAndStop(1);

        // Just like we have a ship to contain our ship movie...
        addChild(_explodeMovie);
        this.x = x;
        this.y = y;
        this.rotation = rot;
    }

    public function endExplode (event :Event) :void
    {
        if (_explodeMovie.currentFrame >= (_explodeMovie.totalFrames - 1) &&
            _frameCount++ >= (_isSmall ? SM_EXPLODE_FRAMES : EXPLODE_FRAMES)) {
            _explodeMovie.stop();
            _explodeMovie.removeEventListener(Event.ENTER_FRAME, endExplode);
            parent.removeChild(this);
        }
    }

    protected var _parent :BoardSprite;

    protected var _explodeMovie :MovieClipAsset;

    protected var _frameCount :int = 0;

    protected var _isSmall :Boolean;

    [Embed(source="rsrc/ship_explosion_small.swf")]
    protected var smExplodeAnim :Class;

    /** Why do we freaking need this crap??? No way to tell when we finish. */
    protected static const EXPLODE_FRAMES :int = 8;
    protected static const SM_EXPLODE_FRAMES :int = 6;
}
}
