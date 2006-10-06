package {

import flash.display.Sprite;
import flash.display.Shape;

import flash.events.Event;

import flash.geom.Point;

import mx.core.MovieClipAsset;

public class BoardSprite extends Sprite
{
    public var boardWidth :int;
    public var boardHeight :int;

    public function BoardSprite (board :Board, ships :Array) :void
    {
        this.boardWidth = board.width;
        this.boardHeight = board.height;

        _obstacles = board.obstacles;
        _ships = ships;
        paint();
    }

    /**
     * Sets the center of the screen.  We need to adjust ourselves to match.
     */
    public function setAsCenter (boardX :Number, boardY :Number) :void
    {
        x = StarFight.WIDTH/2 - (boardX*Codes.PIXELS_PER_TILE);
        y = StarFight.HEIGHT/2 - (boardY*Codes.PIXELS_PER_TILE);
    }

    /**
     * Returns the first collision for something of the specified radius (rad)
     *  moving along the given path.  ignoreShip is a ship ID to ignore, or
     *  we  ignore all if -1.
     */
    public function getCollision (oldX :Number, oldY :Number,
        newX :Number, newY :Number, rad :Number, ignoreShip :int) :Collision
    {
        var hits :Array = [];

        /** The first one we've seen so far. */
        var bestTime :Number = 1.0;
        var bestHit :Collision = null;

        var dx :Number = newX - oldX;
        var dy :Number = newY - oldY;

        if (ignoreShip >= 0) {
            // Check each ship and figure out which one we hit first.
            for (var ii :int; ii < _ships.length; ii++) {
                if (ii == ignoreShip || (dx == 0 && dy == 0)) {
                    continue;
                }
                var ship :ShipSprite = _ships[ii];
                if (ship == null) {
                    continue;
                }

                var bX :Number = ship.boardX;
                var bY :Number = ship.boardY;
                var rad :Number = ShipSprite.COLLISION_RAD;
                // We approximate a ship as a circle for this...
                var a :Number = dx*dx + dy*dy;
                var b :Number = 2*(dx*(oldX-bX) + dy*(oldY-bY));
                var c :Number = bX*bX + bY*bY + oldX*oldX + oldY*oldY -
                    2*(bX*oldX + bY*oldY) - rad*rad;

                var determ :Number = b*b - 4*a*c;
                if (determ >= 0.0) {
                    var u :Number = (-b - Math.sqrt(determ))/(2*a);
                    if ((u >= 0.0) && (u <= 1.0)) {
                        if (u < bestTime) {
                            bestTime = u;
                            bestHit = new Collision(ship, u, false);
                        }
                    }                    
                }
            }
        }

        // Check each obstacle and figure out which one we hit first.
        for each (var obs :Obstacle in _obstacles) {
            if ((obs.bX <= (newX + rad)) &&
                ((obs.bX+1.0) >= (newX - rad)) &&
                (obs.bY <= (newY + rad)) &&
                ((obs.bY+1.0) >= (newY - rad))) {

                // Find how long it is til our X coords collide.
                var timeToX :Number;
                if (dx > 0.0) {
                    timeToX = (obs.bX - (oldX+rad))/dx;
                } else if (dx < 0.0) {
                    timeToX = ((obs.bX+1.0) - (oldX-rad))/dx;
                } else {
                    timeToX = 0.0;
                }

                // Find how long it is til our Y coords collide.
                var timeToY :Number;
                if (dy > 0.0) {
                    timeToY = (obs.bY - (oldY+rad))/dy;
                } else if (dy < 0.0) {
                    timeToY = ((obs.bY+1.0) - (oldY-rad))/dy;
                } else {
                    timeToY = 0.0;
                }
                var time :Number = Math.max(timeToX, timeToY);
                
                if (time < bestTime) {
                    bestTime = time;
                    bestHit = new Collision(obs, time, timeToX > timeToY);
                }
            }
        }
        return bestHit;
    }

    /**
     * Returns a valid starting point for a ship which is clear.
     */
    public function getStartingPos () :Point
    {
        var pt :Point;
        while (true) {
            pt = new Point(Math.random() * (boardWidth - 4) + 2,
                Math.random() * (boardHeight - 4) + 2);
            if (getCollision(pt.x, pt.y, pt.x, pt.y, ShipSprite.COLLISION_RAD,
                    -1) == null) {
                return pt;
            }
        }

        // Should never reach here.
        return null;
    }

    public function explode (x :Number, y :Number, rot :int,
        isSmall :Boolean) :void
    {
        var exp :Explosion = new Explosion(x * Codes.PIXELS_PER_TILE,
            y * Codes.PIXELS_PER_TILE, rot, isSmall, this);
        addChild(exp);
    }

    /**
     * Draw the board.
     */
    public function paint () :void
    {
        var mask :Shape = new Shape();
        addChild(mask);
        mask.graphics.clear();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, boardWidth*Codes.PIXELS_PER_TILE,
            boardHeight*Codes.PIXELS_PER_TILE);
        mask.graphics.endFill();
        this.mask = mask;

        _spaceMovie = MovieClipAsset(new spaceAnim());
        _spaceMovie.gotoAndStop(1);
        //MSTaddChild(_spaceMovie);

        for each (var obs :Obstacle in _obstacles) {
            addChild(obs);
        }
    }

    /** All the obstacles in the world. */
    protected var _obstacles :Array;

    /** Reference to the array of ships we know about. */
    protected var _ships :Array;

    /** Our ship animation. */
    protected var _spaceMovie :MovieClipAsset;

    [Embed(source="rsrc/space.swf")]
    protected var spaceAnim :Class;
}
}
