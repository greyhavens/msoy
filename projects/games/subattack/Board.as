package {

import flash.events.Event;

import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

public class Board
{
    /** The dimensions of the board. */
    public static const WIDTH :int = 60;
    public static const HEIGHT :int = 30;

    public function Board (gameObj :EZGame, seaDisplay :SeaDisplay)
    {
        _gameObj = gameObj;
        _seaDisplay = seaDisplay;
        _gameObj.addEventListener(StateChangedEvent.GAME_STARTED, gameDidStart);
        _gameObj.addEventListener(MessageReceivedEvent.TYPE, msgReceived);

        // create a submarine for each player
        var names :Array = gameObj.getPlayerNames();
        var sub :Submarine;
        for (var ii :int = 0; ii < names.length; ii++) {
            var xx :int = getStartingX(ii);
            var yy :int = getStartingY(ii);

            sub = new Submarine(ii, String(names[ii]), xx, yy, this);
            _seaDisplay.addChild(sub);
            _subs[ii] = sub;

            // mark this sub's 
            _traversable[coordsToIdx(xx, yy)] = true;
            _seaDisplay.markTraversable(xx, yy);
        }

        // if we're a player, put our submarine last, so that it
        // shows up always on top of other submarines
        var myIndex :int = gameObj.getMyIndex();
        if (myIndex != -1) {
            sub = (_subs[myIndex] as Submarine);
            _seaDisplay.setChildIndex(sub, _seaDisplay.numChildren - 1);
            _seaDisplay.setFollowSub(sub);
        }

        _seaDisplay.addEventListener(Event.ENTER_FRAME, enterFrame);
    }

    /**
     * Is the specified tile traversable?
     */
    public function isTraversable (xx :int, yy :int) :Boolean
    {
        return (xx >= 0) && (xx < WIDTH) && (yy >= 0) && (yy < HEIGHT) &&
            Boolean(_traversable[coordsToIdx(xx, yy)]);
    }

    /**
     * Called by the game to respawn the current player.
     */
    public function respawn () :void
    {
        var playerIndex :int = _gameObj.getMyIndex();
        var sub :Submarine = (_subs[playerIndex] as Submarine);
        if (!sub.isDead()) {
            return; // don't try to respawn
        }

        // scan through the entire array and remember the location furthest
        // away from other subs
        var bestx :int = sub.getX();
        var besty :int = sub.getY();
        var bestDist :Number = 0;
        for (var yy :int = 0; yy < HEIGHT; yy++) {
            for (var xx :int = 0; xx < WIDTH; xx++) {
                if (Boolean(_traversable[coordsToIdx(xx, yy)])) {
                    var minDist :Number = Number.MAX_VALUE;
                    for each (var otherSub :Submarine in _subs) {
                        if (otherSub != sub) {
                            minDist = Math.min(minDist,
                                otherSub.distance(xx, yy));
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

        _gameObj.sendMessage("spawn" + playerIndex, coordsToIdx(bestx, besty));
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

                _gameObj.localChat(killer.getPlayerName() + " has shot " +
                    sub.getPlayerName());
            }
        }

        // if it exploded in bounds, make that area traversable
        if (xx >= 0 && xx < WIDTH && yy >= 0 && yy < HEIGHT) {
            // mark the board area as traversable there
            _traversable[coordsToIdx(xx, yy)] = true;
            _seaDisplay.markTraversable(xx, yy);

            var duration :int = (killCount == 0) ? 200 : 400;
            _seaDisplay.addChild(new Explosion(xx, yy, duration, this));
        }

        return killCount;
    }

    protected function coordsToIdx (x :int, y :int) :int
    {
        return (y * WIDTH) + x;
    }

    /**
     * Handles game did start, and that's it.
     */
    protected function gameDidStart (event :StateChangedEvent) :void
    {
        // player 0 starts the ticker
        if (_gameObj.getMyIndex() == 0) {
            _gameObj.startTicker("tick", 100);
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

/*
            if (_ticks < MAX_QUEUED_TICKS) {
                _ticks++;
            } else {
                doTick();
            }
        }
            */
    }

    protected function processAction (name :String, value :int) :void
    {
        if (name.indexOf("sub") == 0) {
            var subIndex :int = int(name.substring(3));
            var moveResult :Boolean = Submarine(_subs[subIndex]).performAction(
                value);
            if (!moveResult) {
                trace("Dropped action: " + name);
            }

        } else if (name.indexOf("spawn") == 0) {
            var spawnIndex :int = int(name.substring(5));
            Submarine(_subs[spawnIndex]).respawn(
                int(value % WIDTH), int(value / WIDTH));
        }
    }

    protected function doTick () :void
    {
        var array :Array = (_ticks.shift() as Array);
        for (var ii :int = 0; ii < array.length; ii += 2) {
            processAction(String(array[ii]), int(array[ii + 1]));
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

        // check to see if any torpedos are hitting subs
        torpsCopy = _torpedos.concat();
        for each (torp in torpsCopy) {
            var xx :int = torp.getX();
            var yy :int = torp.getY();
            for each (sub in _subs) {
                if (!sub.isDead() && (xx == sub.getX()) && (yy == sub.getY())) {
                    // we have a hit!
                    torp.explode(); // will end up calling torpedoExploded
                    break; // break the inner loop
                }
            }
        }

        // now let each sub enact any queued moves
        for each (sub in _subs) {
            sub.postTick();
            if (_gameObj.getMyIndex() == 0) {
                if (sub.getScore() > 4) {
                    _gameObj.sendChat(sub.getPlayerName() + " is the winner!");
                    _gameObj.endGame(sub.getPlayerIndex());
                    return;
                }
            }
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
            return (WIDTH - 1);

        case 2:
            return 0;

        case 3:
            return (WIDTH - 1);

        case 4:
            return 0;

        case 5:
            return (WIDTH - 1);

        case 6:
            return (WIDTH / 2);

        case 7:
            return (WIDTH / 2);
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
            return (HEIGHT - 1);

        case 2:
            return (HEIGHT - 1);

        case 3:
            return 0;

        case 4:
            return (HEIGHT / 2);

        case 5:
            return (HEIGHT / 2);

        case 6:
            return 0;

        case 7:
            return (HEIGHT - 1);
        }
    }

    /** The game object. */
    protected var _gameObj :EZGame;

    /** The 'sea' where everything lives. */
    protected var _seaDisplay :SeaDisplay;
    
    protected var _ticks :Array = [];

    /** Contains the submarines, indexed by player index. */
    protected var _subs :Array = [];

    /** Contains active torpedos, in no particular order. */
    protected var _torpedos :Array = [];

    /** An array tracking the traversability of each tile. */
    protected var _traversable :Array = [];

    protected static const MAX_QUEUED_TICKS :int = 5;
}
}
