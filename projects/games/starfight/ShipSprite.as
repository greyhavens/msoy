package {

import flash.display.Sprite;

import flash.events.KeyboardEvent;
import flash.events.Event;

import flash.geom.Point;

import flash.utils.ByteArray;

import flash.media.Sound;

import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFieldAutoSize;

import flash.events.TimerEvent;

import flash.utils.Timer;

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
    public static const KV_ENTER :uint = 13;

    /** The size of the ship. */
    public static const WIDTH :int = 40;
    public static const HEIGHT :int = 40;
    public static const COLLISION_RAD :Number = 0.9;

    /** Powerup flags. */
    public static const SPEED_MASK :int = 1 << Powerup.SPEED;
    public static const SPREAD_MASK :int = 1 << Powerup.SPREAD;
    public static const SHIELDS_MASK :int = 1 << Powerup.SHIELDS;

    /** "frames" within the actionscript for movement animations. */
    public static const IDLE :int = 1;
    public static const FORWARD :int = 3;
    public static const REVERSE :int = 2;

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

    /** Type of ship we're using. */
    public var shipType :int;

    /** The player name for the ship. */
    public var playerName :String;

    /** The ship's current score. */
    public var score :int;

    /**
     * Constructs a new ship.  If skipStartingPos, don't bother finding an
     *  empty space to start in.
     */
    public function ShipSprite (board :BoardSprite, game :StarFight,
        skipStartingPos :Boolean, shipId :int, name :String,
        isOwnShip :Boolean)
    {
        accel = 0.0;
        turnRate = 0.0;
        turnAccelRate = 0.0;
        xVel = 0.0;
        yVel = 0.0;
        power = 1.0; // full
        powerups = 0;
        score = 0;
        if (_isOwnShip && _shieldSound != null) {
            _shieldSound.gotoAndStop(2);
            _shieldSound = null;
        }
        this.shipId = shipId;
        playerName = name;
        _isOwnShip = isOwnShip;
        shipType = 0;

        if (!skipStartingPos) {
            var pt :Point = board.getStartingPos();
            boardX = pt.x;
            boardY = pt.y;
        }

        _board = board;
        _game = game;

        /** Used to rotate our ship itself without touching associated info. */
        _ship = new Sprite();
        addChild(_ship);

        setShipType(shipType);

        // Play the spawn sound.
        if (_game != null) {
            var sound :Sound = Codes.SHIP_TYPES[shipType].SPAWN;
            _game.playSoundAt(sound, boardX, boardY);
        }

        // Add our name as a textfield
        var nameText :TextField = new TextField();
        nameText.autoSize = TextFieldAutoSize.CENTER;
        nameText.selectable = false;
        nameText.x = 0;
        nameText.y = HEIGHT/2;

        var format:TextFormat = new TextFormat();
        format.font = "Verdana";
        format.color = Codes.CYAN;
        format.size = 12;
        nameText.defaultTextFormat = format;
        nameText.text = playerName;
        addChild(nameText);
    }

    /**
     * Move one tick's worth of distance on its current heading.
     */
    public function move (time :Number) :void
    {
        var friction :Number = Math.pow(getFriction(), time);
        var accelFact :Number = accel * time;

        xVel = xVel*friction + Math.cos(_ship.rotation*Codes.DEGS_TO_RADS)*accelFact;
        yVel = yVel*friction + Math.sin(_ship.rotation*Codes.DEGS_TO_RADS)*accelFact;

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

            var sound :Sound;
            switch (obstacle.type) {
            case Obstacle.ASTEROID_1:
            case Obstacle.ASTEROID_2:
                sound = Sounds.ASTEROID_COLLIDE;
                break;
            case Obstacle.JUNK:
                sound = Sounds.JUNK_COLLIDE;
                break;
            case Obstacle.WALL:
            default:
                sound = Sounds.METAL_COLLIDE;
                break;
            }
            _game.playSoundAt(sound, startX + dx * coll.time,
                startY + dy * coll.time);

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
    public function hit (shooterId :int, shooterType :int) :void
    {
        // Already dead, don't bother.
        if (power <= DEAD) {
            return;
        }

        var hitPower :Number = Codes.SHIP_TYPES[shooterType].hitPower /
            Codes.SHIP_TYPES[shipType].armor;

        power -= ((powerups & SHIELDS_MASK) ? hitPower/2 : hitPower);
        if (power <= DEAD) {
            _game.explode(boardX, boardY, _ship.rotation, shooterId, shipType);

            // Turn off sound loops.
            if (_thrusterSound != null) {
                _thrusterSound.gotoAndStop(2);
                _thrusterSound = null;
            }

            if (_shieldSound != null) {
                _shieldSound.gotoAndStop(2);
                _shieldSound = null;
            }

            _engineSound.gotoAndStop(2);
        
            setVisible(false);

            // Stop moving and firing.
            xVel = 0;
            yVel = 0;
            turnRate = 0;
            accel = 0;
            turnAccelRate = 0;
            _firing = false;

            // After a 5 second interval, reposition & reset.
            var timer :Timer = new Timer(RESPAWN_DELAY, 1);
            timer.addEventListener(TimerEvent.TIMER, restart);
            timer.start();
        }
    }

    /**
     * Positions the ship at a brand new spot after exploding and resets its
     *  dynamics.
     */
    public function restart (event :TimerEvent) :void
    {
        power = 1.0; //full
        powerups = 0;
        var pt :Point = _board.getStartingPos();
        boardX = pt.x;
        boardY = pt.y;
        xVel = 0;
        yVel = 0;
        turnRate = 0;
        accel = 0;
        turnAccelRate = 0;
        _ship.rotation = 0;

        _engineSound.gotoAndStop(1);

        _game.forceStatusUpdate();
        setVisible(true);
    }

    protected function setVisible (visible :Boolean) :void
    {
        if (this.visible != visible) {
            this.visible = visible;

            if (visible) {
                var sound :Sound = Codes.SHIP_TYPES[shipType].SPAWN;
                _game.playSoundAt(sound, boardX, boardY);
            }
        }
    }

    /**
     * Returns the ship's friction factor.
     */
    public function getFriction () :Number
    {
        // Maybe make this different per ship type.
        return Codes.SHIP_TYPES[shipType].friction;
    }

    /**
     * Process the movement of the ship for this timestep.
     */
    public function tick (time :Number) :void
    {
        var turnFriction :Number =
            Math.pow(Codes.SHIP_TYPES[shipType].turnFriction, time);

        turnRate = turnRate * turnFriction + turnAccelRate;
        turn(turnRate*time);

        move(time);
        if (accel > 0.0) {
            setAnimMode(FORWARD, false);
        } else if (accel < 0.0) {
            setAnimMode(REVERSE, false);
        } else {
            setAnimMode(IDLE, false);
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
    public function setAnimMode (mode :int, force :Boolean) :void
    {
        if (force || _shipMovie.currentFrame != mode) {
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
        _ship.rotation += degCW;
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
        // Can't do squat while dead.
        if (power <= DEAD) {
            return;
        }

        if (event.keyCode == KV_LEFT) {
            turnAccelRate = -Codes.SHIP_TYPES[shipType].turnAccelRate;
        } else if (event.keyCode == KV_RIGHT) {
            turnAccelRate = Codes.SHIP_TYPES[shipType].turnAccelRate;
        } else if (event.keyCode == KV_UP) {
            accel = ((powerups & SPEED_MASK) ?
                Codes.SHIP_TYPES[shipType].forwardAccel*SPEED_BOOST_FACTOR :
                Codes.SHIP_TYPES[shipType].forwardAccel);

            if (_isOwnShip) {
                // Play the thruster sound, stop any old thrustering.
                if (_thrusterSound != null && _thrusterRev) {
                    _thrusterSound.gotoAndStop(2);
                    _thrusterSound = null;
                }
                if (_thrusterSound == null) {
                    _thrusterRev = false;
                    _thrusterSound = Sounds.THRUSTER_MOV;
                    _thrusterSound.gotoAndStop(1);
                }
            }

        } else if (event.keyCode == KV_DOWN) {
            accel = ((powerups & SPEED_MASK) ?
                Codes.SHIP_TYPES[shipType].backwardAccel*SPEED_BOOST_FACTOR :
                Codes.SHIP_TYPES[shipType].backwardAccel);

            if (_isOwnShip) {
                // Play the thruster sound, stop any old thrustering.
                if (_thrusterSound != null && !_thrusterRev) {
                    _thrusterSound.gotoAndStop(2);
                    _thrusterSound = null;
                }

                if (_thrusterSound == null) {
                    _thrusterRev = true;
                    _thrusterSound = Sounds.THRUSTER_RETRO_MOV;
                    _thrusterSound.gotoAndStop(1);
                }
            }

        } else if (event.keyCode == KV_SPACE) {
            if (_ticksToFire <= 0) {
                fire();
            }
            _firing = true;
        }
    }

    /**
     * Sets up our sprites and such for our given shiptype.
     */
    public function setShipType (type :int) :void
    {
        if (type != shipType || _shipMovie == null) {
            shipType = type;

            // Remove any old movies of other types of ship.
            if (_shipMovie != null) {
                _ship.removeChild(_shipMovie);
                _ship.removeChild(_shieldMovie);
            }

            // Set up our animation.
            _shipMovie = MovieClipAsset(new Codes.SHIP_TYPES[shipType].SHIP_ANIM());
            _shieldMovie =
                MovieClipAsset(new Codes.SHIP_TYPES[shipType].SHIELD_ANIM());

            setAnimMode(IDLE, true);
            _shipMovie.x = WIDTH/2;
            _shipMovie.y = -HEIGHT/2;
            _shipMovie.rotation = 90;
            _ship.addChild(_shipMovie);

            _shieldMovie.gotoAndStop(1);
            _shieldMovie.x = 55/2;
            _shieldMovie.y = -58/2;
            _shieldMovie.rotation = 90;
            if (powerups & SHIELDS_MASK) {
                _shieldMovie.alpha = 1.0;
            } else {
                _shieldMovie.alpha = 0.0;
            }
            _ship.addChild(_shieldMovie);

            if (_isOwnShip) {
                // Start the engine sound...
                if (_engineSound != null) {
                    _engineSound.gotoAndStop(2);
                    _engineSound = null;
                }

                // Play the engine sound forever til we stop.
                _engineSound = Codes.SHIP_TYPES[shipType].ENGINE_MOV;
                _engineSound.gotoAndStop(1);
            }
        }
    }

    public function fire () :void
    {
        var rads :Number = _ship.rotation*Codes.DEGS_TO_RADS;
        var cos :Number = Math.cos(rads);
        var sin :Number = Math.sin(rads);

        var shotX :Number = cos * SHOT_SPD + xVel;
        var shotY :Number = sin * SHOT_SPD + yVel;

        var shotVel :Number = Math.sqrt(shotX*shotX + shotY*shotY);
        var shotAngle :Number = Math.atan2(shotY, shotX);

        var type :int = (powerups & SPREAD_MASK) ? ShotSprite.SPREAD :
            ShotSprite.NORMAL;

        _game.fireShot(boardX + cos * COLLISION_RAD, boardY + sin * COLLISION_RAD,
            shotVel, shotAngle, shipId, shipType, type);

        _ticksToFire = TICKS_PER_SHOT - 1;
    }

    /**
     * Register that a key was released - we only care about the arrows.
     */
    public function keyReleased (event :KeyboardEvent) :void
    {
        // Can't do squat while dead.
        if (power <= DEAD) {
            return;
        }

        if (event.keyCode == KV_LEFT) {
            turnAccelRate = Math.max(turnAccelRate, 0);
        } else if (event.keyCode == KV_RIGHT) {
            turnAccelRate = Math.min(turnAccelRate, 0);
        } else if (event.keyCode == KV_UP) {
            accel = Math.min(accel, 0);

            // Stop our sound if appropriate.
            if (_isOwnShip && _thrusterSound != null && !_thrusterRev) {
                _thrusterSound.gotoAndStop(2);
                _thrusterSound = null;
            }
        } else if (event.keyCode == KV_DOWN) {
            accel = Math.max(accel, 0);

            // Stop our sound if appropriate.
            if (_isOwnShip && _thrusterSound != null && _thrusterRev) {
                _thrusterSound.gotoAndStop(2);
                _thrusterSound = null;
            }
        } else if (event.keyCode == KV_SPACE) {
            _firing = false;
        }
    }

    /**
     * Give a powerup to the ship.
     */
    public function awardPowerup (type :int) :void
    {
        powerups |= (1 << type);

        if (_isOwnShip && type == Powerup.SHIELDS && _shieldSound == null) {
            _shieldSound = Sounds.SHIELDS_MOV;
            _shieldSound.gotoAndStop(1);
        }
    }

    /**
     * Increase the ship's score.
     */
    public function addScore (score :int) :void
    {
        this.score += score;
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
        _ship.rotation = bytes.readShort();
        power = bytes.readFloat();
        powerups = bytes.readInt();
        setShipType(bytes.readInt());
        score = bytes.readInt();
        setVisible(bytes.readBoolean());
    }

    /**
     * Forces the ship to point upwards (for display purposes).
     */
    public function pointUp () :void
    {
        _ship.rotation = -90;
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

        if (Math.abs(dX) < 0.5) {
            xVel += dX/(Codes.FRAMES_PER_UPDATE*2);
        } else {
            boardX = report.boardX;
        }

        // Maybe let boardY float if we're not too far off.
        var dY :Number = report.boardY - boardY;
        if (Math.abs(dY) < 0.5) {
            yVel += dY/(Codes.FRAMES_PER_UPDATE*2);
        } else {
            boardY = report.boardY;
        }

        turnRate = report.turnRate;

        // Maybe let rotation float if we're not too far off.
        var dTheta :Number = report._ship.rotation - _ship.rotation;
        if (Math.abs(dTheta) < 45) {
            turnRate += dTheta/(Codes.FRAMES_PER_UPDATE*2);
        } else {
            _ship.rotation = report._ship.rotation;
        }

        // These we always update exactly as reported.
        power = report.power;
        powerups = report.powerups;
        setShipType(report.shipType);
        score = report.score;
        setVisible(report.visible);
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
        bytes.writeShort(_ship.rotation);
        bytes.writeFloat(power);
        bytes.writeInt(powerups);
        bytes.writeInt(shipType);
        bytes.writeInt(score);
        bytes.writeBoolean(visible);

        return bytes;
    }

    /** The board we inhabit. */
    protected var _board :BoardSprite;

    /** The main game object. */
    protected var _game :StarFight;

    /** The sprite with our ship graphics in it. */
    protected var _ship :Sprite;

    protected var _firing :Boolean;
    protected var _ticksToFire :int;

    /** Ship performance characteristics. */
    protected static const SHOT_SPD :Number = 0.5;
    protected static const TICKS_PER_SHOT :int = 8;
    protected static const SPEED_BOOST_FACTOR :Number = 1.5;
    protected static const RESPAWN_DELAY :int = 3000;
    protected static const DEAD :Number = 0.001;

    /** Sounds currently being played - only play sounds for ownship. Note
     * that due to stupid looping behavior these need to be MovieClips to keep
     * from getting gaps between loops. */
    protected var _engineSound :MovieClipAsset;
    protected var _thrusterSound :MovieClipAsset;
    protected var _shieldSound :MovieClipAsset;

    /** Animations. */
    protected var _shipMovie :MovieClipAsset;
    protected var _shieldMovie :MovieClipAsset;

    /** State of thurster sounds. */
    protected var _thrusterRev :Boolean;

    /** Whether this is ourselves. */
    protected var _isOwnShip :Boolean;

}
}
