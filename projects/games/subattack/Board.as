package {

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
     * @return an Array of the subs hit.
     */
    public function torpedoExploded (torpedo :Torpedo) :Array
    {
        // remove it from our list of torpedos
        var idx :int = _torpedos.indexOf(torpedo);
        if (idx == -1) {
            trace("OMG! Unable to find torpedo??");
            return null;
        }
        _torpedos.splice(idx, 1); // remove that torpedo

        _seaDisplay.removeChild(torpedo); // TODO

        var xx :int = torpedo.getX();
        var yy :int = torpedo.getY();
        var subs :Array = [];

        // TODO find all the subs that were affected

        // if it exploded in bounds, make that area traversable
        if (xx >= 0 && xx < WIDTH && yy >= 0 && yy < HEIGHT) {
            // mark the board area as traversable there
            _traversable[coordsToIdx(xx, yy)] = true;
            _seaDisplay.markTraversable(xx, yy);
        }

        return subs;
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
            doTick();

        } else if (name.indexOf("sub") == 0) {
            var subIndex :int = int(name.substring(3));
            var moveResult :Boolean = Submarine(_subs[subIndex]).performAction(
                int(event.value));
            if (!moveResult) {
                trace("Dropped action: " + name);
            }
        }
    }

    protected function doTick () :void
    {
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
                if ((xx == sub.getX()) && (yy == sub.getY())) {
                    // we have a hit!
                    torp.explode(); // will end up calling torpedoExploded
                    break; // break the inner loop
                }
            }
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
        }
    }

    /** The game object. */
    protected var _gameObj :EZGame;

    /** The 'sea' where everything lives. */
    protected var _seaDisplay :SeaDisplay;

    /** Contains the submarines, indexed by player index. */
    protected var _subs :Array = [];

    /** Contains active torpedos, in no particular order. */
    protected var _torpedos :Array = [];

    /** An array tracking the traversability of each tile. */
    protected var _traversable :Array = [];
}
}
