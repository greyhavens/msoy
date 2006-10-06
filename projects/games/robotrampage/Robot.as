package {

import flash.display.Sprite;
import flash.display.Scene;

import flash.events.MouseEvent;
import flash.events.TimerEvent;

import flash.utils.ByteArray;
import flash.utils.Timer;
import flash.utils.getTimer;

import mx.core.BitmapAsset;
import mx.core.MovieClipAsset;
import mx.controls.Label;

import mx.effects.Glow;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

public class Robot extends Sprite
{

    public function Robot (robotFactory :RobotFactory, rr :RobotRampage)
    {
        _robotFactory = robotFactory;
        _rr = rr;

        _style = new Array(PART_COUNT);
        _parts = new Array(PART_COUNT);
        for (var ii :int = 0; ii < PART_COUNT; ii++) {
            _parts[ii] = null;
        }

        _target = null;
        _state = STATE_IDLE;

        randomizeRobot();

        addEventListener(MouseEvent.MOUSE_OVER, mouseOver);
        addEventListener(MouseEvent.MOUSE_OUT, mouseOut);
        addEventListener(MouseEvent.CLICK, mouseClick);
    }

    /**
     * Unserialize our data from a byte array.
     */
    public function readFrom (bytes :ByteArray) :void
    {
        x = bytes.readFloat();
        y = bytes.readFloat();
        _target = _rr.getBase(bytes.readInt());
        _state = bytes.readInt();
        var ii :int;
        for (ii = 0; ii < PART_COUNT; ii++) {
            _style[ii] = bytes.readInt();
            updatePart(ii);
        }

        sortBodyParts ();
    }

    /**
     * Serialize our data to a byte array.
     */
    public function writeTo (bytes :ByteArray) :ByteArray
    {
        bytes.writeFloat(x);
        bytes.writeFloat(y);
        bytes.writeInt(_target == null ? -1 : _target.getPlayerIndex());
        bytes.writeInt(_state);
        for (var ii :int = 0; ii < PART_COUNT; ii++) {
            bytes.writeInt(_style[ii]);
        }

        return bytes;
    }

    /**
     * Point our robot at an unsuspecting moon base.
     */
    public function setTarget (base :MoonBase) : void
    {
        _target = base;
        _state = (_target != null) ? STATE_WALKING : STATE_IDLE;
    }

    /**
     * A beat of or cold, robotic heart.
     */
    public function tick () :void
    {
        if (_target != null && _target.isDestroyed()) {
            setTarget(null);
        }

        // TODO: Try and fall into rank with my robot bretheren

        switch (_state) {
        case STATE_IDLE:
            paceIdly();
            break;
        case STATE_WALKING:
            walkTowardsTarget();
            break;
        case STATE_EXPLODING:
            // TODO: swap to explosion animation
            break;
        }
    }

    public function isNearTarget () :Boolean
    {
        if (_target == null) {
            return false;
        }

        var dx :int = Math.abs(x - _target.x);
        var dy :int = Math.abs(y - _target.y);

        if ((dx*dx + dy*dy) <= DESTRUCTION_RADIUS_SQUARED) {
            return true;
        }

        return false;
    }

    /**
     * Makes the robot walk towards its target.
     */
    protected function walkTowardsTarget () :void
    {
        /* FIXME: Walk less stupidly. Although I suppose they ARE dumb robots,
         * and they DO look pretty amusing jittering along their path....
         */
        if (_target.x < x) {
            x -= ROBOT_STEP_SIZE;
        } else if (_target.x > x) {
            x += ROBOT_STEP_SIZE;
        }

        if (_target.y < y) {
            y -= ROBOT_STEP_SIZE;
        } else if (_target.y > y) {
            y += ROBOT_STEP_SIZE;
        }
    }

    /**
     * Makes the robot pace around in no particular direction, because it has
     * no place to go.
     */
    protected function paceIdly () :void 
    {
        /* TODO: Maybe some fancier pacing than total random jitter.
         * But then again, it's pretty funny stuff
         */
        switch (int(Math.random() * 3)) {
        case 0:
            x++;
            break;
        case 1:
            x--;
            break;
        default:
            // Do nothing
        }

        switch (int(Math.random() * 3)) {
        case 0:
            y++;
            break;
        case 1:
            y--;
            break;
        default:
            // Do nothing
        }
    }

    /**
     * Picks an aspect of the robot at random, and changes it to a new random
     * value. Note: this might not actually change anything, since the new
     * part might get turned to the same value as the old part.
     */
    public function randomizeOnePart () :void
    {
        randomizeRobot(int(Math.random() * PART_COUNT));
    }
    
    /**
     * Randomizes the specified aspect of the robot, or if part is
     * omitted, the whole body and color.
     */
    public function randomizeRobot (part :int = -1) :void
    {
        var parts :Array;

        if (part == -1) {
            parts = [PART_COLOR, PART_HEAD, PART_TORSO, PART_LEGS];
        } else {
            parts = [part];
        }

        for each (part in parts) {
            // FIXME: Maybe enforce that it really changes?
            _style[part] = int(Math.random() * STYLE_OPTIONS);
        }

        if (parts[0] == PART_COLOR) {
            // If color changed, change the whole body
            parts = [PART_HEAD, PART_TORSO, PART_LEGS];
        }

        for each (part in parts) {
            updatePart(part);
        }

        sortBodyParts ();
    }

    /**
     * Updates the specified part of the robot, swapping to the appropriate 
     * artwork.
     */
    public function updatePart (partType :int) :void
    {
        if (partType == PART_COLOR) {
            return;
        }

        var color :int = _style[PART_COLOR];

        if (_parts[partType] != null) {
            removeChild(_parts[partType]);
        }

        _parts[partType] =
            _robotFactory.getPart(color, partType, _style[partType]);
        _parts[partType].x = - (_parts[partType].width / 2 );
        _parts[partType].y = - _parts[partType].height;
        addChild(_parts[partType]);

        // TODO: cut to the transition animation for this part
    }

    /**
     * Hacky debug function to print some text on the screen.
     */
    protected var _labelY :int = 0;
    protected function makeLabel (text :String) :void
    {
        var label :TextField = new TextField();
        label.autoSize = TextFieldAutoSize.LEFT;
        label.background = true;
        label.selectable = false;
        label.text = text;
        label.x = 0;
        label.y = _labelY;
        label.width = label.textWidth;
        addChild(label);

        _labelY += label.height;
    }

    /**
     * KABOOOOOOOM!
     */
    public function explode () :void
    {
        if (_target == null) {
            // Huh? We were told to explode, but we don't have a target?
            return;
        }

        _state = STATE_EXPLODING;
        _target.takeDamage();

        /* TODO: do a real explosion animation, but for now we'll just sit him
         * here for a moment and then say we're done blowing up
         */
        var timer :Timer = new Timer(500, 1);
        timer.addEventListener(TimerEvent.TIMER, finishExploding);
        timer.start();
    }

    protected function finishExploding (event :TimerEvent) :void
    {
        _state = STATE_DEAD;
    }

    /**
     * Returns whether or not we're done doing our fancy little explosion
     * animation and can be shuffled off this mortal coil.
     */
    public function isDead () :Boolean
    {
        return _state == STATE_DEAD;
    }

    /**
     * Sorts the display order of our body parts so things draw properly.
     */
    protected function sortBodyParts () :void
    {
        for (var ii :int = 0; ii < PART_COUNT; ii++) {
            if (_parts[ii] != null) {
                setChildIndex(_parts[ii], 0);
            }
        }
    }

    /**
     * Handles things when the mouse comes over the robot.
     */
    protected function mouseOver (event :MouseEvent) :void
    {
        if (!_selected) {
            setGlow(true);
        }
    }

    /**
     * Handles things when the mouse leaves from over the robot.
     */
    protected function mouseOut (event :MouseEvent) :void
    {
        if (!_selected) {
            setGlow(false);
        }
    }

    /**
     * Handles mouse clicks on the robot.
     */
    protected function mouseClick (event :MouseEvent) :void
    {
        //randomizeRobot();
        toggleSelected();
    }

    protected function toggleSelected () :void
    {
        _selected = !_selected;

        if (_selected) {
            _rr.selectRobot(this);
            setGlow(true);
        } else {
            _rr.unselectRobot(this);
            setGlow(false);
        }
    }

    /**
     * Turn on or off the glow surrounding this robot.
     */
    protected function setGlow (doGlow :Boolean) :void
    {
        // nuke any existing glow
        if (_glow != null) {
            _glow.end();
            _glow = null;

            filters = new Array();
        }
        // if things are already in the proper state, do nothing
        if (doGlow == (_glow != null)) {
            return;
        }

        // add a glow if appropriate
        if (doGlow) {
            _glow = new Glow(this);
            _glow.alphaFrom = 0;
            _glow.alphaTo = 1;
            _glow.blurXFrom = 0;
            _glow.blurXTo = GLOW_RADIUS;
            _glow.blurYFrom = 0;
            _glow.blurYTo = GLOW_RADIUS;
            _glow.color = _selected ? GLOW_COLOR_SELECTED : GLOW_COLOR_HOVER;
            _glow.duration = 200;
            _glow.play();
        }
    }

    public function getPartStyle (part :int) :int
    {
        return _style[part];
    }

    public static function isValidSet (robots :Array) :Boolean
    {
        var r1 :Robot = robots[0];
        var r2 :Robot = robots[1];
        var r3 :Robot = robots[2];

        for (var ii :int = 0; ii < PART_COUNT; ii++) {
            if (! ((r1.getPartStyle(ii) == r2.getPartStyle(ii) && 
                    r2.getPartStyle(ii) == r3.getPartStyle(ii)) ||
                   (r1.getPartStyle(ii) != r2.getPartStyle(ii) &&
                    r1.getPartStyle(ii) != r3.getPartStyle(ii) &&
                    r2.getPartStyle(ii) != r3.getPartStyle(ii)))) {
                return false;
            }
        }

        return true;
    }

    public function unselect () :void
    {
        _selected = false;
        setGlow(false);
    }

    /** What this robot is currently doing. */
    protected var _state :int;

    /** Whether or not this robot is selected by the user. */
    protected var _selected :Boolean = false;

    /** The player this robot is currently targetting, or -1 for none. */
    protected var _target :MoonBase;

    /** An array of ints describing this robot's style. */
    protected var _style :Array;

    /** Our robot's parts. */
    protected var _parts :Array;

    /** A factory for building robots. */
    protected var _robotFactory :RobotFactory;

    /** Our top level game object. */
    protected var _rr :RobotRampage;

    /** Identifier for the color of our robot. */
    protected static const PART_COLOR :int = 0;

    /** Identifier for the robot's head. */
    protected static const PART_HEAD :int = 1;

    /** Identifier for the robot's torso. */
    protected static const PART_TORSO :int = 2;

    /** Identifier for the robot's legs. */
    protected static const PART_LEGS :int = 3;

    /** How many different parts make up our robot. */
    protected static const PART_COUNT :int = 4;

    /** How many options do we have for each thing in our robot's style. */
    protected static const STYLE_OPTIONS :int = 3;

    /** The glow effect used for mouse hovering. */
    protected var _glow :Glow;

    /** Radius of the glow that surrounds a robot.*/
    protected static const GLOW_RADIUS :int = 20;

    /** Color to glow when we're mousing over a robot. */
    protected static const GLOW_COLOR_HOVER :uint = 0xffff00;

    /** Color to glow when we've selected a robot for our set. */
    protected static const GLOW_COLOR_SELECTED :uint = 0x00ffff;

    /** How close we must be to a base to destroy it, squared. */
    protected static const DESTRUCTION_RADIUS_SQUARED :int = 25;


    protected static const STATE_IDLE :int = 0;
    protected static const STATE_WALKING :int = 1;
    protected static const STATE_EXPLODING :int = 2;
    protected static const STATE_DEAD :int = 3;

    protected static const ROBOT_STEP_SIZE :Number = 1.5;
}
}
