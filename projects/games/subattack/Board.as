package {

import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

public class Board
{
    /** The dimensions of the board. */
    public static const SIZE :int = 16;

    public function Board (gameObj :EZGame, seaDisplay :SeaDisplay)
    {
        _gameObj = gameObj;
        _seaDisplay = seaDisplay;
        _gameObj.addEventListener(StateChangedEvent.GAME_STARTED, gameDidStart);
        _gameObj.addEventListener(MessageReceivedEvent.TYPE, msgReceived);

        // create a submarine for each player
        var names :Array = gameObj.getPlayerNames();
        for (var ii :int = 0; ii < names.length; ii++) {
            var xx :int = getStartingX(ii);
            var yy :int = getStartingY(ii);

            var sub :Submarine = new Submarine(
                ii, String(names[ii]), xx, yy, this);
            _seaDisplay.addChild(sub);
            _subs[ii] = sub;

            // mark this sub's 
            _traversable[coordsToIdx(xx, yy)] = true;
        }
    }

    /**
     * Is the specified tile traversable?
     */
    public function isTraversable (xx :int, yy :int) :Boolean
    {
        return (xx >= 0) && (xx < SIZE) && (yy >= 0) && (yy < SIZE) &&
            Boolean(_traversable[coordsToIdx(xx, yy)]);
    }

    /**
     * Called by a torpedo to notify us that it was added.
     */
    public function torpedoAdded (torpedo :Torpedo) :void
    {
        _torpedos.push(torpedo);
    }

    public function torpedoExploded (torpedo :Torpedo) :void
    {
    }

    protected function coordsToIdx (x :int, y :int) :int
    {
        return (y * SIZE) + x;
    }

    protected function gameDidStart (event :StateChangedEvent) :void
    {
        if (_gameObj.getMyIndex() != 0) {
            return;
        }

        // player 0 is responsible for setting up the initial board
        var board :Array = new Array();
        board.length = SIZE * SIZE;
        for (var ii :int = 0; ii < board.length; ii++) {
            board[ii] = false;
        }

        // set up the board
        _gameObj.set("board", board);

        // start things going
        _gameObj.startTicker("tick", 100);
    }

    /**
     * Handles MessageReceivedEvents.
     */
    protected function msgReceived (event :MessageReceivedEvent) :void
    {
        var name :String = event.name;
        if (name == "tick") {
            // clear out the move counts
            for each (var sub :Submarine in _subs) {
                sub.tick();
            }
            for each (var torp :Torpedo in _torpedos) {
                torp.tick();
            }

        } else if (name.indexOf("sub") == 0) {
            var subIndex :int = int(name.substring(3));
            var moveResult :Boolean = Submarine(_subs[subIndex]).performAction(
                int(event.value));
            if (!moveResult) {
                trace("Dropped action: " + name);
            }
        }
    }

    protected function getStartingX (playerIndex :int) :int
    {
        switch (playerIndex) {
        default:
            trace("Cannot yet handle " + (playerIndex + 1) + " player games!");
            // fall through to 0
        case 0:
            return 0;

        case 1:
            return (SIZE - 1);
        }
    }

    protected function getStartingY (playerIndex :int) :int
    {
        switch (playerIndex) {
        default:
            // don't bother logging again
            // fall through to 0
        case 0:
            return 0;

        case 1:
            return (SIZE - 1);
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
