package {

import flash.display.Sprite;

import flash.events.KeyboardEvent;

import flash.geom.Point;

import flash.utils.ByteArray;

import mx.core.MovieClipAsset;

/**
 * Represents a single ships (ours or opponent's) in the world.
 */
public class ShipSprite extends Sprite
{
    /** Some useful key codes. */
    public static const KV_LEFT :uint = 37;
    public static const KV_UP :uint = 38;
    public static const KV_RIGHT :uint = 39;
    public static const KV_DOWN :uint = 40;
    public static const KV_SPACE :uint = 32;

    /** The size of the ship. */
    public static const WIDTH :int = 40;
    public static const HEIGHT :int = 40;
    public static const COLLISION_RAD :Number = 0.9;

    /** How fast the ship is accelerating. */
    public var accel :Number;

    /** The ship's instantaneous velocity. */
    public var xVel :Number;
    public var yVel :Number;

    /** The location of the ship on the board. */
    public var boardX :Number;
    public var boardY :Number;

    /** How fast are we currently turning. */
    public var turnRate :Number;

    /** our id. */
    public var shipId :int;

    /**
     * Constructs a new ship.  If skipStartingPos, don't bother finding an
     *  empty space to start in.
     */
    public function ShipSprite (board :BoardSprite, game :StarFight,
        skipStartingPos :Boolean, shipId :int)
    {
        accel = 0.0;
        turnRate = 0.0;
        xVel = 0.0;
        yVel = 0.0;
        this.shipId = shipId;

        if (!skipStartingPos) {
            var pt :Point = board.getStartingPos();
            boardX = pt.x;
            boardY = pt.y;
        }

        _board = board;
        _game = game;

        // Set up our animation.
        _shipMovie = MovieClipAsset(new shipAnim());
        setAnimMode(IDLE);
        _shipMovie.x = WIDTH/2;
        _shipMovie.y = -HEIGHT/2;
        _shipMovie.rotation = 90;
        addChild(_shipMovie);
    }

    /**
     * Move one tick's worth of distance on its current heading.
     */
    public function move (time :Number) :void
    {
        var friction :Number = Math.pow(getFriction(), time);
        var accelFact :Number = accel * time;

        xVel = xVel*friction + Math.cos(rotation*Codes.DEGS_TO_RADS)*accelFact;
        yVel = yVel*friction + Math.sin(rotation*Codes.DEGS_TO_RADS)*accelFact;

        resolveMove(boardX, boardY, boardX + xVel*time, boardY + yVel*time);
    }

    /**
     * Try to move the ship between the specified points, reacting to any
     *  collisions along the way.  This function calls itself recursively
     *  to resolve collisions created in the rebound from earlier collisions.
     */
    public function resolveMove (startX :Number, startY :Number,
        endX :Number, endY :Number) :void
    {
        var coll :Collision = _board.getCollision(startX, startY, endX, endY,
            COLLISION_RAD, -1);
        if (coll != null) {
            var obstacle :Obstacle = Obstacle(coll.hit);
            var bounce :Number = obstacle.getElasticity();
            var dx :Number = endX - startX;
            var dy :Number = endY - startY;
            if (coll.isHoriz) {
                xVel = -xVel * bounce;
                if (coll.time < 0.1) {
                    boardX = startX;
                    boardY = startY;
                } else {
                    resolveMove(startX + dx * coll.time, startY + dy * coll.time,
                        startX + dx * coll.time - dx * (1.0-coll.time) * bounce,
                        endY);
                }
            } else { // vertical bounce
                yVel = -yVel * bounce;
                if (coll.time < 0.1) {
                    boardX = startX;
                    boardY = startY;
                } else {
                    resolveMove(startX + dx * coll.time,
                        startY + dy * coll.time, endX,
                        startY + dy * coll.time -
                        dy * (1.0-coll.time) * bounce);
                }
            }
        } else {
            // Everything's happy - no collisions.
            boardX = endX;
            boardY = endY;
        }
    }

    /**
     * Returns the ship's friction factor.
     */
    public function getFriction () :Number
    {
        // TODO: Make this different per ship.
        return FRICTION;
    }

    /**
     * Process the movement of the ship for this timestep.
     */
    public function tick (time :Number) :void
    {
        turn(turnRate*time);

        move(time);
        if (accel > 0.0) {
            setAnimMode(FORWARD);
        } else if (accel < 0.0) {
            setAnimMode(REVERSE);
        } else {
            setAnimMode(IDLE);
        }

        if (_firing && (_ticksToFire <= 0)) {
            fire();
        } else if (_ticksToFire > 0) {
            _ticksToFire -= time;
        }
    }

    /**
     * Sets our animation to show forward/idle/reverse
     */
    protected function setAnimMode (mode :int) :void
    {
        if (_shipMovie.currentFrame != mode) {
            //TODO : re-enable
            _shipMovie.gotoAndStop(mode);
        }
    }

    /**
     * Turns the ship by the angle specified in degrees clockwise (negative
     *  for CCW)
     */
    public function turn (degCW :Number) :void
    {
        rotation += degCW;
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

    /**
     * Register that a key was pressed.  We only care about arrows.
     */
    public function keyPressed (event :KeyboardEvent) :void
    {
        if (event.keyCode == KV_LEFT) {
            turnRate = -TURN_RATE;
        } else if (event.keyCode == KV_RIGHT) {
            turnRate = TURN_RATE;
        } else if (event.keyCode == KV_UP) {
            accel = FORWARD_ACCEL;
        } else if (event.keyCode == KV_DOWN) {
            accel = BACKWARD_ACCEL;
        } else if (event.keyCode == KV_SPACE) {
            // TODO : firing mode rather than instantaneous.
            if (_ticksToFire <= 0) {
                fire();
            }
            _firing = true;
        }
    }

    public function fire () :void
    {
        var rads :Number = rotation*Codes.DEGS_TO_RADS;
        var cos :Number = Math.cos(rads);
        var sin :Number = Math.sin(rads);
        _game.fireShot(boardX + cos, boardY + sin,
            cos * SHOT_SPD, sin * SHOT_SPD, shipId);
        _ticksToFire = TICKS_PER_SHOT - 1;
    }

    /**
     * Register that a key was released - we only care about the arrows.
     */
    public function keyReleased (event :KeyboardEvent) :void
    {
        if (event.keyCode == KV_LEFT) {
            turnRate = Math.max(turnRate, 0);
        } else if (event.keyCode == KV_RIGHT) {
            turnRate = Math.min(turnRate, 0);
        } else if (event.keyCode == KV_UP) {
            accel = Math.min(accel, 0);
        } else if (event.keyCode == KV_DOWN) {
            accel = Math.max(accel, 0);
        } else if (event.keyCode == KV_SPACE) {
            _firing = false;
        }
    }

    /**
     * Unserialize our data from a byte array.
     */
    public function readFrom (bytes :ByteArray) :void
    {
        accel = bytes.readFloat();
        xVel = bytes.readFloat();
        yVel = bytes.readFloat();
        boardX = bytes.readFloat();
        boardY = bytes.readFloat();
        turnRate = bytes.readFloat();
        rotation = bytes.readShort();
    }

    /**
     * Serialize our data to a byte array.
     */
    public function writeTo (bytes :ByteArray) :ByteArray
    {
        bytes.writeFloat(accel);
        bytes.writeFloat(xVel);
        bytes.writeFloat(yVel);
        bytes.writeFloat(boardX);
        bytes.writeFloat(boardY);
        bytes.writeFloat(turnRate);
        bytes.writeShort(rotation);

        return bytes;
    }

    /** The board we inhabit. */
    protected var _board :BoardSprite;

    /** The main game object. */
    protected var _game :StarFight;

    protected var _firing :Boolean;
    protected var _ticksToFire :int;

    /** Various UI constants. */
    protected static const RED :uint = uint(0xFF0000);
    protected static const BLACK :uint = uint(0x000000);

    /** Ship performance characteristics. */
    protected static const TURN_RATE :Number = 7.0;
    protected static const FORWARD_ACCEL :Number = 0.02;
    protected static const BACKWARD_ACCEL :Number = -0.01;
    protected static const FRICTION :Number = 0.95;
    protected static const SHOT_SPD :Number = 0.5;
    protected static const TICKS_PER_SHOT :int = 6;

    /** Our ship animation. */
    protected var _shipMovie :MovieClipAsset;

    [Embed(source="rsrc/ship.swf#ship_movie_01")]
    protected var shipAnim :Class;

    /** "frames" within the actionscript for movement animations. */
    protected static const IDLE :int = 1;
    protected static const FORWARD :int = 3;
    protected static const REVERSE :int = 2;
}
}
