package {

import flash.display.Sprite;

import mx.core.MovieClipAsset;

public class ShotSprite extends Sprite {

    /** Position. */
    public var boardX :Number;
    public var boardY :Number;

    /** Velocity. */
    public var xVel :Number;
    public var yVel :Number;

    public var complete :Boolean;

    public function ShotSprite (x :Number, y :Number,
        xVel :Number, yVel :Number) :void
    {
        boardX = x;
        boardY = y;
        this.xVel = xVel;
        this.yVel = yVel;

        complete = false;

        _shotMovie = MovieClipAsset(new shotAnim());
        _shotMovie.gotoAndPlay(1);
        _shotMovie.x = 0;
        _shotMovie.y = 0;
        _shotMovie.rotation = 180/Math.PI*Math.atan2(xVel, -yVel);
        addChild(_shotMovie);
    }

    public function tick (board :BoardSprite) :void
    {
        var coll :Collision = board.getCollision(boardX, boardY,
            boardX + xVel, boardY + yVel, COLLISION_RAD);
        if (coll == null) {
            boardX += xVel;
            boardY += yVel;
        } else {
            // TODO: Animate explosion if ship.
            complete = true;
        }
    }

    /**
     * Sets the sprite position for this ship based on its board pos and
     *  another pos which will be the center of the screen.
     */
    public function setPosRelTo (otherX :Number, otherY: Number) :void
    {
        x = ((boardX - otherX) * Codes.PIXELS_PER_TILE) + StarFight.WIDTH/2;
        y = ((boardY - otherY) * Codes.PIXELS_PER_TILE) + StarFight.HEIGHT/2;
    }

    protected static const COLLISION_RAD :Number = 0.1;

    /** Our shot animation. */
    protected var _shotMovie :MovieClipAsset;

    [Embed(source="rsrc/beam.swf")]
    protected var shotAnim :Class;

}
}
