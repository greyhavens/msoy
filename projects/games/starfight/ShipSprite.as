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

    /** Powerup flags. */
    public static const SPEED_MASK :int = 1 << Powerup.SPEED;
    public static const SPREAD_MASK :int = 1 << Powerup.SPREAD;
    public static const SHIELDS_MASK :int = 1 << Powerup.SHIELDS;

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

    /** How fast are we changing our turn rate. */
    public var turnAccelRate :Number;

    /** Our current health */
    public var power :Number;

    /** All the powerups we've got. */
    public var powerups :int;

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
        turnAccelRate = 0.0;
        xVel = 0.0;
        yVel = 0.0;
        power = 1.0; // full
        powerups = 0;
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

        _shieldMovie = MovieClipAsset(new shieldAnim());
        _shieldMovie.gotoAndStop(1);
        _shieldMovie.x = 55/2;
        _shieldMovie.y = -58/2;
        _shieldMovie.rotation = 90;
        addChild(_shieldMovie);
        
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
     * Registers that the ship was hit.
     */
    public function hit () :void
    {
        power -= ((powerups & SHIELDS_MASK) ? HIT_POWER/2 : HIT_POWER);
        if (power <= 0.0) {
            _game.explode(boardX, boardY, rotation);
            power = 1.0; //full
            powerups = 0;
            var pt :Point = _board.getStartingPos();
            boardX = pt.x;
            boardY = pt.y;
            rotation = 0;
        }
    }

    /**
     * Returns the ship's friction factor.
     */
    public function getFriction () :Number
    {
        // Maybe make this different per ship type.
        return FRICTION;
    }

    /**
     * Process the movement of the ship for this timestep.
     */
    public function tick (time :Number) :void
    {
        var turnFriction :Number = Math.pow(TURN_FRICTION, time);
        
        turnRate = turnRate * turnFriction + turnAccelRate;
        turn(turnRate*time);

        move(time);
        if (accel > 0.0) {
            setAnimMode(FORWARD);
        } else if (accel < 0.0) {
            setAnimMode(REVERSE);
        } else {
            setAnimMode(IDLE);
        }

        if (powerups & SHIELDS_MASK) {
            _shieldMovie.alpha = 1.0;
        } else {
            _shieldMovie.alpha = 0.0;
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
            turnAccelRate = -TURN_ACCEL_RATE;
        } else if (event.keyCode == KV_RIGHT) {
            turnAccelRate = TURN_ACCEL_RATE;
        } else if (event.keyCode == KV_UP) {
            accel = ((powerups & SPEED_MASK) ? FORWARD_ACCEL*1.3 :
                FORWARD_ACCEL);
        } else if (event.keyCode == KV_DOWN) {
            accel = ((powerups & SPEED_MASK) ? BACKWARD_ACCEL*1.3 :
                BACKWARD_ACCEL);
        } else if (event.keyCode == KV_SPACE) {
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

        var shotX :Number = cos * SHOT_SPD + xVel;
        var shotY :Number = sin * SHOT_SPD + yVel;

        var shotVel :Number = Math.sqrt(shotX*shotX + shotY*shotY);
        var shotAngle :Number = Math.atan2(shotY, shotX);

        var type :int = (powerups & SPREAD_MASK) ? ShotSprite.SPREAD :
            ShotSprite.NORMAL;

        _game.fireShot(boardX + cos, boardY + sin,
            shotVel, shotAngle, shipId, type);

        _ticksToFire = TICKS_PER_SHOT - 1;
    }

    /**
     * Register that a key was released - we only care about the arrows.
     */
    public function keyReleased (event :KeyboardEvent) :void
    {
        if (event.keyCode == KV_LEFT) {
            turnAccelRate = Math.max(turnAccelRate, 0);
        } else if (event.keyCode == KV_RIGHT) {
            turnAccelRate = Math.min(turnAccelRate, 0);
        } else if (event.keyCode == KV_UP) {
            accel = Math.min(accel, 0);
        } else if (event.keyCode == KV_DOWN) {
            accel = Math.max(accel, 0);
        } else if (event.keyCode == KV_SPACE) {
            _firing = false;
        }
    }

    public function awardPowerup (type :int) :void
    {
        powerups |= (1 << type);
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
        turnAccelRate = bytes.readFloat();
        rotation = bytes.readShort();
        power = bytes.readFloat();
        powerups = bytes.readInt();
    }

    /**
     * Update our ship to the reported position, BUT if possible try to
     *  set ourselves up to make up for any discrepancy smoothly.
     */
    public function updateForReport (report :ShipSprite) :void
    {
        accel = report.accel;
        xVel = report.xVel;
        yVel = report.yVel;
        
        // Maybe let boardX float if we're not too far off.
        var dX :Number = report.boardX - boardX;
        
        Logger.log("dX: " + dX);

        if (Math.abs(dX) < 0.5) {
            xVel += dX/(StarFight.FRAMES_PER_UPDATE*2);
        } else {
            boardX = report.boardX;
        }

        // Maybe let boardY float if we're not too far off.
        var dY :Number = report.boardY - boardY;
        if (Math.abs(dY) < 0.5) {
            yVel += dY/(StarFight.FRAMES_PER_UPDATE*2);
        } else {
            boardY = report.boardY;
        }

        turnRate = report.turnRate;

        // Maybe let rotation float if we're not too far off.
        var dTheta :Number = report.rotation - rotation;
        if (Math.abs(dTheta) < 45) {
            turnRate += dTheta/(StarFight.FRAMES_PER_UPDATE*2);
        } else {
            rotation = report.rotation;
        }

        // These we always update exactly as reported.
        power = report.power;
        powerups = report.powerups;
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
        bytes.writeFloat(turnAccelRate);
        bytes.writeShort(rotation);
        bytes.writeFloat(power);
        bytes.writeInt(powerups);

        return bytes;
    }

    /** The board we inhabit. */
    protected var _board :BoardSprite;

    /** The main game object. */
    protected var _game :StarFight;

    protected var _firing :Boolean;
    protected var _ticksToFire :int;

    /** Ship performance characteristics. */
    protected static const TURN_ACCEL_RATE :Number = 1.0;
    protected static const FORWARD_ACCEL :Number = 0.02;
    protected static const BACKWARD_ACCEL :Number = -0.01;
    protected static const FRICTION :Number = 0.95;
    protected static const TURN_FRICTION :Number = 0.8;
    protected static const SHOT_SPD :Number = 0.5;
    protected static const TICKS_PER_SHOT :int = 8;
    protected static const HIT_POWER :Number = 0.25;

    /** Our ship animation. */
    protected var _shipMovie :MovieClipAsset;
    protected var _shieldMovie :MovieClipAsset;

    [Embed(source="rsrc/ship.swf#ship_movie_01")]
    protected var shipAnim :Class;

    [Embed(source="rsrc/ship_shield.swf")]
    protected var shieldAnim :Class;

    /** "frames" within the actionscript for movement animations. */
    protected static const IDLE :int = 1;
    protected static const FORWARD :int = 3;
    protected static const REVERSE :int = 2;
}
}
