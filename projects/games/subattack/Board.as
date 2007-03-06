package {

import flash.events.Event;

import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

import com.whirled.WhirledGameControl;

public class Board
{
    public function Board (gameCtrl :WhirledGameControl, seaDisplay :SeaDisplay)
    {
        _gameCtrl = gameCtrl;
        _seaDisplay = seaDisplay;

        var playerIds :Array = _gameCtrl.seating.getPlayerIds();
        var playerCount :int = playerIds.length;
        _width = int(DIMENSIONS[playerCount][0]);
        _height = int(DIMENSIONS[playerCount][1]);

        _maxDeaths = playerCount * 5;

        _seaDisplay.setupSea(_width, _height);

        var ii :int;
        for (ii = _width * _height - 1; ii >= 0; ii--) {
            _traversable[ii] = SHOTS_TO_DESTROY;
        }

        // create a submarine for each player
        var sub :Submarine;
        for (ii = 0; ii < playerIds.length; ii++) {
            var playerId :int = (playerIds[ii] as int);
            var xx :int = getStartingX(ii);
            var yy :int = getStartingY(ii);

            sub = new Submarine(
                ii, _gameCtrl.getOccupantName(playerId), xx, yy, this);
            _gameCtrl.getUserCookie(playerId, sub.gotPlayerCookie);
            _seaDisplay.addChild(sub);
            _subs[ii] = sub;

            // mark this sub's starting location as traversable
            setTraversable(xx, yy);
        }

        // if we're a player, put our submarine last, so that it
        // shows up always on top of other submarines
        var myIndex :int = gameCtrl.seating.getMyPosition();
        if (myIndex != -1) {
            sub = (_subs[myIndex] as Submarine);
            _seaDisplay.setChildIndex(sub, _seaDisplay.numChildren - 1);
            _seaDisplay.setFollowSub(sub);
        }

        _seaDisplay.addEventListener(Event.ENTER_FRAME, enterFrame);

        _gameCtrl.addEventListener(MessageReceivedEvent.TYPE, msgReceived);
        if (gameCtrl.isInPlay()) {
            gameDidStart(null);
        } else {
            _gameCtrl.addEventListener(StateChangedEvent.GAME_STARTED,
                gameDidStart);
            _gameCtrl.addEventListener(StateChangedEvent.GAME_ENDED,
                gameDidEnd);
        }
    }

    /**
     * Is the specified tile traversable?
     */
    public function isTraversable (xx :int, yy :int) :Boolean
    {
        return (xx >= 0) && (xx < _width) && (yy >= 0) && (yy < _height) &&
            (0 == int(_traversable[coordsToIdx(xx, yy)]));
    }

    /**
     * Called by a submarine to respawn.
     */
    public function respawn (sub :Submarine) :void
    {
        // scan through the entire array and remember the location furthest
        // away from other subs and torpedos
        var bestx :int = sub.getX();
        var besty :int = sub.getY();
        var bestDist :Number = 0;
        for (var yy :int = 0; yy < _height; yy++) {
            for (var xx :int = 0; xx < _width; xx++) {
                if (0 == int(_traversable[coordsToIdx(xx, yy)])) {
                    var minDist :Number = Number.MAX_VALUE;
                    for each (var otherSub :Submarine in _subs) {
                        if (otherSub != sub && !otherSub.isDead()) {
                            minDist = Math.min(minDist,
                                otherSub.distance(xx, yy));
                        }
                    }
                    for each (var torp :Torpedo in _torpedos) {
                        var checkDist :Boolean = false;
                        switch (torp.getOrient()) {
                        case Action.UP:
                            checkDist = (torp.getX() == xx) &&
                                (torp.getY() >= yy);
                            break;

                        case Action.DOWN:
                            checkDist = (torp.getX() == xx) &&
                                (torp.getY() <= yy);
                            break;

                        case Action.LEFT:
                            checkDist = (torp.getY() == yy) &&
                                (torp.getX() >= xx);
                            break;

                        case Action.RIGHT:
                            checkDist = (torp.getY() == yy) &&
                                (torp.getX() <= xx);
                            break;
                        }

                        if (checkDist) {
                            minDist = Math.min(minDist,
                                torp.distance(xx, yy));
                        }
                    }
                    if (minDist > bestDist) {
                        bestDist = minDist;
                        bestx = xx;
                        besty = yy;
                    }
                }
            }
        }

        // we've found such a great spot, and all the other clients
        // should find the same spot
        sub.respawn(bestx, besty);
    }

    /**
     * Called by a torpedo to notify us that it was added.
     */
    public function torpedoAdded (torpedo :Torpedo) :void
    {
        _torpedos.push(torpedo);
        _seaDisplay.addChild(torpedo);
    }

    /**
     * Called by a torpedo when it has exploded.
     *
     * @return the number of subs hit by the explosion.
     */
    public function torpedoExploded (torpedo :Torpedo) :int
    {
        // remove it from our list of torpedos
        var idx :int = _torpedos.indexOf(torpedo);
        if (idx == -1) {
            trace("OMG! Unable to find torpedo??");
            return 0;
        }
        _torpedos.splice(idx, 1); // remove that torpedo
        _seaDisplay.removeChild(torpedo);

        // find all the subs affected
        var killer :Submarine = torpedo.getOwner();
        var killCount :int = 0;
        var xx :int = torpedo.getX();
        var yy :int = torpedo.getY();
        for each (var sub :Submarine in _subs) {
            if (!sub.isDead() && sub.getX() == xx && sub.getY() == yy) {
                sub.wasKilled();
                killCount++;
                _totalDeaths++;

                _gameCtrl.localChat(killer.getPlayerName() + " has shot " +
                    sub.getPlayerName());

                if (killer.getPlayerIndex() == _gameCtrl.seating.getMyPosition()) {
                    var flowAvailable :Number = _gameCtrl.getAvailableFlow();
                    trace("Available flow at time of kill: " + flowAvailable);
                    var awarded :int = int(flowAvailable * .75);
                    trace("Awarding: " + awarded);
                    _gameCtrl.awardFlow(awarded);
                }
            }
        }

        // if it exploded in bounds, make that area traversable
        if (xx >= 0 && xx < _width && yy >= 0 && yy < _height) {
            // mark the board area as traversable there
            incTraversable(xx, yy);
            _seaDisplay.addChild(new Explode(xx, yy, this));
        }

        return killCount;
    }

    protected function coordsToIdx (xx :int, yy :int) :int
    {
        return (yy * _width) + xx;
    }

    protected function setTraversable (xx :int, yy :int) :void
    {
        _traversable[coordsToIdx(xx, yy)] = 0;
        _seaDisplay.markTraversable(xx, yy, 0, isTraversable(xx, yy - 1),
            isTraversable(xx, yy + 1));
    }

    protected function incTraversable (xx :int, yy :int) :void
    {
        var idx :int = coordsToIdx(xx, yy);
        var val :int = int(_traversable[idx]);
        if (val > 0) {
            val--;
            _traversable[idx] = val;
            _seaDisplay.markTraversable(xx, yy, val, isTraversable(xx, yy - 1),
                isTraversable(xx, yy + 1));
        }
    }

    /**
     * Handles game did start, and that's it.
     */
    protected function gameDidStart (event :StateChangedEvent) :void
    {
        // player 0 starts the ticker
        if (_gameCtrl.seating.getMyPosition() == 0) {
            _gameCtrl.startTicker("tick", 100);
        }
    }

    protected function gameDidEnd (event :StateChangedEvent) :void
    {
        var mydex :int = _gameCtrl.seating.getMyPosition();
        if (mydex >= 0) {
            _gameCtrl.setUserCookie(Submarine(_subs[mydex]).getNewCookie());
        }
    }

    /**
     * Handles MessageReceivedEvents.
     */
    protected function msgReceived (event :MessageReceivedEvent) :void
    {
        var name :String = event.name;
        if (name == "tick") {
            _ticks.push(new Array());

            if (_ticks.length > MAX_QUEUED_TICKS) {
                doTick(); // do one now...
            }

        } else {
            // add any actions received during this tick
            var array :Array = (_ticks[_ticks.length - 1] as Array);
            array.push(event.name);
            array.push(event.value);
        }
    }

    protected function processAction (name :String, actions :Array) :void
    {
        if (name.indexOf("sub") == 0) {
            var subIndex :int = int(name.substring(3));
            var sub :Submarine = Submarine(_subs[subIndex]);
            for each (var action :int in actions) {
                sub.performAction(action);
            }
        }
    }

    protected function doTick () :void
    {
        var array :Array = (_ticks.shift() as Array);
        for (var ii :int = 0; ii < array.length; ii += 2) {
            processAction(String(array[ii]), (array[ii + 1] as Array));
        }

        var sub :Submarine;
        var torp :Torpedo;
        // tick all subs and torps
        for each (sub in _subs) {
            sub.tick();
        }
        var torpsCopy :Array = _torpedos.concat();
        for each (torp in torpsCopy) {
            // this may explode a torpedo if it hits seaweed or a wall
            torp.tick();
        }

        // then we check torpedo-on-torpedo action, and pass-through
        checkTorpedos();

        // find the highest scoring players
        var winners :Array;
        var hiScore :int = int.MIN_VALUE;
        for each (sub in _subs) {
            var score :int = sub.getScore();
            if (score > hiScore) {
                hiScore = score;
                winners = [];
            }
            if (score == hiScore) {
                winners.push(sub.getPlayerIndex());
            }
        }

        // maybe end the game and declare them winners
        if (hiScore >= 5 || _totalDeaths >= _maxDeaths) {
            _gameCtrl.endGame(winners);
        }
    }

    protected function checkTorpedos () :void
    {
        // check to see if any torpedos are hitting subs
        var torp :Torpedo;
        var sub :Submarine;
        var xx :int;
        var yy :int;
        var exploders :Array = [];
        for each (torp in _torpedos) {
            xx = torp.getX();
            yy = torp.getY();
            for each (sub in _subs) {
                if (!sub.isDead() && (xx == sub.getX()) && (yy == sub.getY())) {
                    // we have a hit!
                    if (-1 == exploders.indexOf(torp)) {
                        exploders.push(torp);
                    }
                    break; // break the inner loop (one sub is enough!)
                }
            }

            for each (var torp2 :Torpedo in _torpedos) {
                if (torp != torp2 && torp.willExplode(torp2)) {
                    if (-1 == exploders.indexOf(torp)) {
                        exploders.push(torp);
                    }
                    if (-1 == exploders.indexOf(torp2)) {
                        exploders.push(torp2);
                    }
                }
            }
        }

        // now explode any torps that need it
        for each (torp in exploders) {
            torp.explode();
        }
    }

    /**
     * Handles Event.ENTER_FRAME.
     */
    protected function enterFrame (event :Event) :void
    {
        if (_ticks.length > 1) {
            doTick();
        }
    }

    /**
     * Return the starting x coordinate for the specified player.
     */
    protected function getStartingX (playerIndex :int) :int
    {
        switch (playerIndex) {
        default:
            trace("Cannot yet handle " + (playerIndex + 1) + " player games!");
            // fall through to 0
        case 0:
            return 0;

        case 1:
            return (_width - 1);

        case 2:
            return 0;

        case 3:
            return (_width - 1);

        case 4:
            return 0;

        case 5:
            return (_width - 1);

        case 6:
            return (_width / 2);

        case 7:
            return (_width / 2);
        }
    }

    /**
     * Return the starting y coordinate for the specified player.
     */
    protected function getStartingY (playerIndex :int) :int
    {
        switch (playerIndex) {
        default:
            // don't bother logging again
            // fall through to 0
        case 0:
            return 0;

        case 1:
            return (_height - 1);

        case 2:
            return (_height - 1);

        case 3:
            return 0;

        case 4:
            return (_height / 2);

        case 5:
            return (_height / 2);

        case 6:
            return 0;

        case 7:
            return (_height - 1);
        }
    }

    /** The game Control. */
    protected var _gameCtrl :WhirledGameControl;

    /** The 'sea' where everything lives. */
    protected var _seaDisplay :SeaDisplay;

    protected var _totalDeaths :int = 0;

    /** The maximum number of deaths before we end the game. */
    protected var _maxDeaths :int;

    /** The width of the board. */
    protected var _width :int;

    /** The height of the board. */
    protected var _height :int;
    
    protected var _ticks :Array = [];

    /** Contains the submarines, indexed by player index. */
    protected var _subs :Array = [];

    /** Contains active torpedos, in no particular order. */
    protected var _torpedos :Array = [];

    /** An array tracking the traversability of each tile. */
    protected var _traversable :Array = [];

    protected static const DIMENSIONS :Array = [
        [  0,  0 ], // 0 player game
        [ 10, 10 ], // 1 player game
        [ 50, 25 ], // 2 player game
        [ 60, 30 ], // 3 player game
        [ 75, 30 ], // 4 player game
        [ 75, 40 ], // 5 player game
        [ 80, 40 ], // 6 player game
        [ 80, 50 ], // 7 player game
        [ 90, 50 ]  // 8 players!
    ];

    protected static const MAX_QUEUED_TICKS :int = 5;

    protected static const SHOTS_TO_DESTROY :int = 1; // 2;
}
}
