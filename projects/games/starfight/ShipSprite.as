package {

import flash.display.Sprite;

import flash.events.KeyboardEvent;

import flash.geom.Point;

import flash.utils.ByteArray;

import mx.core.MovieClipAsset;

public class ShipSprite extends Sprite
{
    /** Some useful key codes. */
    public static const KV_LEFT :uint = 37;
    public static const KV_UP :uint = 38;
    public static const KV_RIGHT :uint = 39;
    public static const KV_DOWN :uint = 40;

    /** The size of the ship. */
    public static const WIDTH :int = 40;
    public static const HEIGHT :int = 40;

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

    public function ShipSprite (board :BoardSprite)
    {
        accel = 0.0;
        turnRate = 0.0;
        xVel = 0.0;
        yVel = 0.0;

        var pt :Point = board.getStartingPos();
        boardX = pt.x;
        boardY = pt.y;

        _board = board;

        _shipMovie = MovieClipAsset(new shipAnim());
        setAnimMode(IDLE);
        _shipMovie.gotoAndStop(IDLE); // TODO : remove this
        _shipMovie.x = WIDTH/2;
        _shipMovie.y = -HEIGHT/2;
        _shipMovie.rotation = 90;
        addChild(_shipMovie);
    }

    /**
     * Move one tick's worth of distance on its current heading.
     */
    public function move () :void
    {
        var friction :Number = getFriction();

        xVel = xVel*friction + Math.cos(rotation*Math.PI/180)*accel;
        yVel = yVel*friction + Math.sin(rotation*Math.PI/180)*accel;

        resolveMove(boardX, boardY, boardX + xVel, boardY + yVel);
    }

    public function resolveMove (startX :Number, startY :Number,
        endX :Number, endY :Number) :void
    {
        var coll :Collision = _board.getCollision(startX, startY, endX, endY);
        if (coll != null) {
            var bounce :Number = coll.obstacle.getElasticity();
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
    public function tick () :void
    {
        StarFight.log("Ticking");
        turn(turnRate);

        move();
        if (accel > 0.0) {
            setAnimMode(FORWARD);
        } else if (accel < 0.0) {
            setAnimMode(REVERSE);
        } else {
            setAnimMode(IDLE);
        }

        StarFight.log ("after tick x,y: " + boardX + ", " + boardY);
    }
    
    protected function setAnimMode (mode :int) :void
    {
        if (_shipMovie.currentFrame != mode) {
            //TODO : re-enable
            //_shipMovie.gotoAndStop(mode);
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
        x = ((boardX - otherX) * PIXELS_PER_TILE) + StarFight.WIDTH/2;
        y = ((boardY - otherY) * PIXELS_PER_TILE) + StarFight.HEIGHT/2;
    }

    public function paint () :void
    {
        // TODO : use an image here.
        graphics.beginFill(RED);
        graphics.drawCircle(0, 0, HEIGHT/2);

        graphics.lineStyle(1, BLACK);
        graphics.moveTo(0, 0);
        graphics.lineTo(WIDTH/2, 0);
    }

    /**
     * Register that a key was pressed.  We only care about arrows.
     */
    public function keyPressed (event :KeyboardEvent) :void
    {
        StarFight.log("Key Pressed: " + event.keyCode);

        if (event.keyCode == KV_LEFT) {
            turnRate = -TURN_RATE;
        } else if (event.keyCode == KV_RIGHT) {
            turnRate = TURN_RATE;
        } else if (event.keyCode == KV_UP) {
            accel = FORWARD_ACCEL;
        } else if (event.keyCode == KV_DOWN) {
            accel = BACKWARD_ACCEL;
        }
    }

    /**
     * Register that a key was released - we only care about the arrows.
     */
    public function keyReleased (event :KeyboardEvent) :void
    {
        if (event.keyCode == KV_LEFT) {
            StarFight.log("Stop turning left");
            turnRate = Math.max(turnRate, 0);
        } else if (event.keyCode == KV_RIGHT) {
            turnRate = Math.min(turnRate, 0);
        } else if (event.keyCode == KV_UP) {
            accel = Math.min(accel, 0);
        } else if (event.keyCode == KV_DOWN) {
            accel = Math.max(accel, 0);
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

    protected var _board :BoardSprite;

    /** Various UI constants. */
    protected static const RED :uint = uint(0xFF0000);
    protected static const BLACK :uint = uint(0x000000);

    protected static const TURN_RATE :Number = 10.0;
    protected static const FORWARD_ACCEL :Number = 0.05;
    protected static const BACKWARD_ACCEL :Number = -0.025;

    protected static const PIXELS_PER_TILE :int = 20;

    protected static const FRICTION :Number = 0.95;

    protected var _shipMovie :MovieClipAsset;

    [Embed(source="rsrc/ship.swf#ship_movie_01")]
    protected var shipAnim :Class;

    /** "frames" within the actionscript for movement animations. */
    protected static const IDLE :int = 1;
    protected static const FORWARD :int = 3;
    protected static const REVERSE :int = 2;
}
}
