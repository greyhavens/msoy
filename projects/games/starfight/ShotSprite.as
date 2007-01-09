package {

import flash.display.Sprite;

import mx.core.MovieClipAsset;

public class ShotSprite extends Sprite {

    /** Shot types. */
    public static const NORMAL :int = 0;
    public static const SPREAD :int = 1;

    public static const SPREAD_FACT :Number = 0.1;

    /** Position. */
    public var boardX :Number;
    public var boardY :Number;

    /** Velocity. */
    public var xVel :Number;
    public var yVel :Number;

    /** the ships that fired us. */
    public var shipId :int;

    public var complete :Boolean;

    public function ShotSprite (x :Number, y :Number,
        vel :Number, angle :Number, shipId :int, game :StarFight) :void
    {
        boardX = x;
        boardY = y;
        this.xVel = vel * Math.cos(angle);
        this.yVel = vel * Math.sin(angle);
        this.shipId = shipId;

        complete = false;

        _game = game;

        _shotMovie = MovieClipAsset(new shotAnim());
        _shotMovie.gotoAndStop(1);
        _shotMovie.x = -_shotMovie.width/2;
        _shotMovie.y = 0;
        rotation = Codes.RADS_TO_DEGS*Math.atan2(xVel, -yVel);
        addChild(_shotMovie);
    }

    public function tick (board :BoardSprite, time :Number) :void
    {
        var coll :Collision = board.getCollision(boardX, boardY,
            boardX + xVel*time, boardY + yVel*time, COLLISION_RAD, shipId);
        if (coll == null) {
            boardX += xVel*time;
            boardY += yVel*time;
        } else {
            if (coll.hit is ShipSprite) {
                var ship :ShipSprite = ShipSprite(coll.hit);
                _game.hitShip(ship, boardX + (xVel*coll.time*time),
                    boardY + (yVel*coll.time*time));

            } else {
                var obs :Obstacle = Obstacle(coll.hit);
                _game.hitObs(obs, boardX + (xVel*coll.time*time),
                    boardY + (yVel*coll.time*time));
            }
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

    /** Our game. */
    protected var _game :StarFight;

}
}
