package {

import flash.display.Sprite;

import flash.events.TimerEvent;
import flash.external.ExternalInterface;

import flash.utils.Timer;
import flash.utils.getTimer;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

import flash.text.StyleSheet;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import flash.utils.ByteArray;

[SWF(width="800", height="540")]
public class RobotRampage extends Sprite
    implements Game
{
    public function RobotRampage ()
    {
        _robotFactory = new RobotFactory();
    }

    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObj = gameObj;
        _myIndex = _gameObj.getMyIndex();
        _robotInterval = INITIAL_ROBOT_INTERVAL;

        graphics.clear();
        graphics.beginFill(0xCCCCFF);
        graphics.drawRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        graphics.endFill();

        addMoonBases();

        _gameObj.addEventListener(MessageReceivedEvent.TYPE, msgReceived);

        if (isMaster()) {
            for (var ii: int = 0; ii < INITIAL_ROBOTS; ii++) {
                addRobot();
            }

            var timer :Timer = new Timer(125);
            timer.addEventListener(TimerEvent.TIMER, doTick);
            timer.start();
        }
    }

    public function getBase (playerIndex :int) :MoonBase
    {
        if (playerIndex == -1) {
            return null;
        }
        return _bases[playerIndex];
    }

    /**
     * Handles MessageReceivedEvents.
     */
    protected function msgReceived (event :MessageReceivedEvent) :void
    {
        var name :String = event.name;
        if (name == "update") {
            if (!isMaster()) {
                updateBases(event.value[0]);
                updateRobots(event.value[1]);
            }
            sortRobots();
        } else if (name.indexOf("setmade") == 0)  {
           var playerIndex :int = int(name.substring(7));
           makeSet(playerIndex, event.value as Array);

        } else {
            makeLabel("received message: '" + name + "': " + event.value);
        }
    }

    protected function makeSet (playerIndex :int, robotIndices :Array) :void
    {
        for each (var ii :int in robotIndices) {
            var robot :Robot = _robots[ii];

            // FIXME: actually target someone appropriate
            targetRandomBase(robot);
            robot.randomizeOnePart();
        }
    }

    protected function updateBases (bytes :ByteArray) :void
    {
        for each (var base :MoonBase in _bases) {
            base.readFrom(bytes);
        }
    }

    protected function updateRobots (bytes :ByteArray) :void
    {
        var robot :Robot;
        var ii : int;

        
        // Update existing robots
        ii = 0;
        for each (robot in _robots) {
            ii++;
            robot.readFrom(bytes);
        }

        // Add new ones
        ii = 0;
        while (bytes.bytesAvailable != 0) {
            ii++;
            robot = new Robot(_robotFactory, this);
            addChild(robot);
            _robots.push(robot);
            robot.readFrom(bytes);
        }

        // prune dead ones
        for each (robot in _robots) {
            if (robot.isDead()) {
                _robots.splice(_robots.indexOf(robot), 1);
                removeChild(robot);
            }
        }

    }

    protected function doTick (event :TimerEvent) :void
    {
        var robot :Robot;
        for each (robot in _robots) {
            robot.tick();
        }

        _ticksSinceRobot++;

        // Look for robots to explode
        for each (robot in _robots) {
            if (robot.isNearTarget()) {
                robot.explode();
            }
        }

        // Add more robots as appropriate
        if (_ticksSinceRobot >= _robotInterval) {
            if (_robots.length < MAX_ROBOTS) {
                addRobot();
            }
        }

        // And now, in true brute force fashion, we're going to just send
        // the whole state of EVERYTHING.
        sendGameState();
    }

    protected function sendGameState () :void
    {
        var baseData :ByteArray = new ByteArray();
        for each (var base :MoonBase in _bases) {
            base.writeTo(baseData);
        }

        var robotData :ByteArray = new ByteArray();
        for each (var robot :Robot in _robots) {
            robot.writeTo(robotData);
        }


        _gameObj.sendMessage("update", [baseData, robotData]);
    }

    /**
     * Sorts the robots based on their position and then updates their 
     * positioning so they draw in a sane order.
     */
    protected function sortRobots () :void
    {
        _robots.sortOn(["y", "x"]);

        for (var ii :int = 0; ii < _robots.length; ii++)
        {
            setChildIndex(_robots[ii], ii + _bases.length);
        }
    }

    /**
     * Creates our moon bases.
     */
    protected function addMoonBases () : void
    {
        var names :Array = _gameObj.getPlayerNames();
        var count :int = names.length;
        _bases = new Array(count);

        var radius :int = (Math.min(SCREEN_WIDTH, SCREEN_HEIGHT) / 2) - 
            MoonBase.MOON_BASE_RADIUS;

        for (var ii :int = 0; ii < count; ii++) {
            // Make new base
            var base :MoonBase = new MoonBase(_gameObj, String(names[ii]), ii);
            _bases[ii] = base;
            var angle :Number = ii * 2 * Math.PI / count;

            // Place it someplace sane-ish
            /* FIXME: This needs some fancier brains since we want to throw 
             * some perspective on this, so the area immediately in front of 
             * you is bigger.
             * Plus, with that, we always want ourselves to be at the bottom, 
             * viewing the kite (or triangle, or...single line) shaped playfield
             * from that perspective.
             * For now though, meh.
             */
            base.x = (SCREEN_WIDTH/2) - (radius * Math.sin(angle));
            base.y = (SCREEN_HEIGHT/2) + (radius * Math.cos(angle));

            addChild(base);
        }
    }

    /**
     * Adds a new robot with random attributes.
     */
    protected function addRobot (pickTarget :Boolean=false) :void
    {
        var robot :Robot = new Robot(_robotFactory, this);

        robot.x = (width/2) + 
            (Math.random() * 2 * CENTER_RADIUS) - CENTER_RADIUS;
        robot.y = (height/2) + 
            (Math.random() * 2 * CENTER_RADIUS) - CENTER_RADIUS;
        _robots.push(robot);

        if (pickTarget) {
            targetRandomBase(robot);
        }

        _ticksSinceRobot = 0;
        addChild(robot);
    }

    protected function targetRandomBase (robot :Robot) :void
    {
        var bases :Array = [];
        var base :MoonBase;

        for each (base in _bases) {
            if (!base.isDestroyed()) {
                bases.push(base);
            }
        }

        if (bases.length > 0) {
            robot.setTarget(bases[int(Math.random() * bases.length)]);
        } else {
            // All bases are destroyed, but we're still adding robots?
        }
    }

    /** 
     * Returns whether we're serving as the master, forcing our will on all
     * the other clients with an iron fist.
     */
    protected function isMaster () :Boolean
    {
        return _myIndex == 0;
    }

    public function selectRobot (robot :Robot) :void
    {
        _selection.push(robot);

        if (_selection.length == 3) { // FIXME: magic number
            if (Robot.isValidSet(_selection)) {
                // They made a set!
                var data :Array = []
                for each (robot in _selection) {
                    data.push(_robots.indexOf(robot));
                }
                _gameObj.sendMessage("setmade" + _myIndex, data, 0);

            }

            for each (robot in _selection) {
                robot.unselect();
            }

            _selection.length = 0;
        }
    }

    public function unselectRobot (robot :Robot) :void
    {
        var ii :int = _selection.indexOf(robot);
        if (ii != -1) {
            _selection.splice(ii, 1);
        }
    }

    /** Our game object. */
    protected var _gameObj :EZGame;

    /** The index of this particular player. */
    protected var _myIndex :int;

    /** An array of moonbases. */
    protected var _bases :Array;

    /** Number of ticks before we should add another robot. */
    protected var _robotInterval :int;

    /** Number of ticks since we last added a robot. */
    protected var _ticksSinceRobot :int = 0;

    /** An array of robots. */
    protected var _robots :Array = [];

    /** An array of exploding robots. */
    protected var _explodingRobots :Array = [];

    /** A factory for building robots. */
    protected var _robotFactory :RobotFactory;

    /** The currently selected robots. */
    protected var _selection :Array = [];

    /** Width of our play area. */
    protected static const SCREEN_WIDTH :int = 800;

    /** Height of our play area. */
    protected static const SCREEN_HEIGHT :int = 540;

    /** The radius of the circle at the middle of our screen for new robots. */
    protected static const CENTER_RADIUS :int = 
        Math.min(SCREEN_HEIGHT, SCREEN_WIDTH) / 4;

    /** Number of robots to start with. */
    protected static const INITIAL_ROBOTS :int = 6;

    /** Initial robot interval. */
    protected static const INITIAL_ROBOT_INTERVAL :int = 25;

    /** Maximum number of robots to have at any given time. */
    /* FIXME: This should probably vary based on the number of players at a 
     * time. And maybe, grow as the game goes on... By raising the spawn speed
     * and the max robots allowed, it could get pretty psycho and force the
     * game to end. Yay for increasing difficulty.
     */
    protected static const MAX_ROBOTS :int = 21;


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


}
}
