package {

import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;

import flash.events.Event;

import flash.geom.Point;

import com.threerings.util.HashMap;

public class BoardSprite extends Sprite
{
    public var boardWidth :int;
    public var boardHeight :int;

    public var powerupLayer :Sprite;

    public function BoardSprite (board :Board, ships :HashMap,
        powerups :Array) :void
    {
        this.boardWidth = board.width;
        this.boardHeight = board.height;

        _obstacles = board.obstacles;
        _ships = ships;
        _powerups = powerups;
        setupGraphics();
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
            for each (var ship :ShipSprite in _ships.values()) {
                if (ship == null) {
                    continue;
                }

                if (ship.shipId == ignoreShip || (dx == 0 && dy == 0)) {
                    continue;
                }

                var bX :Number = ship.boardX;
                var bY :Number = ship.boardY;
                var r :Number = ShipSprite.COLLISION_RAD + rad;
                // We approximate a ship as a circle for this...
                var a :Number = dx*dx + dy*dy;
                var b :Number = 2*(dx*(oldX-bX) + dy*(oldY-bY));
                var c :Number = bX*bX + bY*bY + oldX*oldX + oldY*oldY -
                    2*(bX*oldX + bY*oldY) - r*r;

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
            
            // Find how long it is til our X coords collide.
            var timeToX :Number;
            if (dx > 0.0) {
                timeToX = (obs.bX - (oldX+rad))/dx;
            } else if (dx < 0.0) {
                timeToX = ((obs.bX+1.0) - (oldX-rad))/dx;
            } else if ((oldX+rad >= obs.bX) && (oldX-rad <= obs.bX+1.0)) {
                timeToX = -1.0; // already there.
            } else {
                timeToX = 2.0; // doesn't hit.
            }
            
            // Find how long it is til our Y coords collide.
            var timeToY :Number;
            if (dy > 0.0) {
                timeToY = (obs.bY - (oldY+rad))/dy;
            } else if (dy < 0.0) {
                timeToY = ((obs.bY+1.0) - (oldY-rad))/dy;
            } else if ((oldY+rad >= obs.bY) && (oldY-rad <= obs.bY+1.0)) {
                timeToY = -1.0; // already there.
            } else {
                timeToY = 2.0; // doesn't hit.
            }

            // Update our bestTime if this is a legitimate collision and is before any
            //  others we've found.
            var time :Number = Math.max(timeToX, timeToY);
            if (time >= 0.0 && time <= 1.0 && time < bestTime &&
                ((timeToX >= 0.0 || (oldX+rad >= obs.bX) && (oldX-rad <= obs.bX+1.0))) &&
                ((timeToY >= 0.0 || (oldY+rad >= obs.bY) && (oldY-rad <= obs.bY+1.0)))){
                bestTime = time;
                bestHit = new Collision(obs, time, timeToX > timeToY);
            }
        }
        return bestHit;
    }

    /** Returns any obstacle at the specified board location. */
    public function getObstacleAt (boardX :int, boardY :int) :Obstacle
    {
        // Check each obstacle and figure out which one we hit first.
        for each (var obs :Obstacle in _obstacles) {
            if (obs.bX == boardX && obs.bY == boardY) {
                return obs;
            }
        }
        
        return null;
    }

    public function getPowerupIdx (oldX :Number, oldY :Number,
        newX :Number, newY :Number, rad :Number) :int
    {

        /** The first one we've seen so far. */
        var bestTime :Number = 1.0;
        var bestHit :int = -1;

        var dx :Number = newX - oldX;
        var dy :Number = newY - oldY;

        // Check each powerup and figure out which one we hit first.
        for (var ii :int; ii < _powerups.length; ii++) {
            var pow :Powerup = _powerups[ii];
            if (pow == null) {
                continue;
            }
            var bX :Number = pow.boardX + 0.5;
            var bY :Number = pow.boardY + 0.5;
            var r :Number = rad + 0.5; // Our radius...
            // We approximate a powerup as a circle for this...
            var a :Number = dx*dx + dy*dy;
            var b :Number = 2*(dx*(oldX-bX) + dy*(oldY-bY));
            var c :Number = bX*bX + bY*bY + oldX*oldX + oldY*oldY -
                2*(bX*oldX + bY*oldY) - r*r;

            var determ :Number = b*b - 4*a*c;
            if (determ >= 0.0) {
                var u :Number = (-b - Math.sqrt(determ))/(2*a);
                if ((u >= 0.0) && (u <= 1.0)) {
                    if (u < bestTime) {
                        bestTime = u;
                        bestHit = ii;
                    }
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
        isSmall :Boolean, shipType :int) :void
    {
        var exp :Explosion = new Explosion(x * Codes.PIXELS_PER_TILE,
            y * Codes.PIXELS_PER_TILE, rot, isSmall, shipType, this);
        addChild(exp);
    }

    /**
     * Draw the board.
     */
    public function setupGraphics () :void
    {
        for each (var obs :Obstacle in _obstacles) {
            addChild(obs);
        }

        addChild(powerupLayer = new Sprite());
    }

    /** All the obstacles in the world. */
    protected var _obstacles :Array;

    /** Reference to the array of ships we know about. */
    protected var _ships :HashMap;

    /** Reference to the array of powerups we know about. */
    protected var _powerups :Array;
}
}
