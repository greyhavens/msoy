package {

import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.StateChangedEvent;

public class Board
{
    /** The dimensions of the board. */
    public static const SIZE :int = 16;

    public function Board (gameObj :EZGame)
    {
        _gameObj = gameObj;
        _gameObj.addEventListener(StateChangedEvent.GAME_STARTED, gameDidStart);
    }

    public function isTraversable (x :int, y :int) :Boolean
    {
        return _gameObj.get("board", coordsToIdx(x, y));
    }

    public function coordsToIdx (x :int, y :int) :int
    {
        return (y * SIZE) + x;
    }

    public function idxToX (index :int) :int
    {
        return (index % SIZE);
    }

    public function idxToY (index :int) :int
    {
        return (index / SIZE);
    }

    protected function gameDidStart (event :StateChangedEvent) :void
    {
        if (_gameObj.getMyIndex() != 0) {
            return;
        }

        var subs :Array = [ ];
        var scores :Array = [ ];

        // player 0 is responsible for setting up the initial board
        var board :Array = new Array();
        board.length = SIZE * SIZE;
        for (var ii :int = 0; ii < board.length; ii++) {
            board[ii] = false;
        }

        // assign starting positions
        // for now, assign the two players to opposite corners
        subs[0] = coordsToIdx(0, 0);
        scores[0] = 0;
        subs[1] = coordsToIdx(SIZE - 1, SIZE - 1);
        scores[1] = 0;
        // and clear out those spots on the board
        for each (var pos :int in subs) {
            board[pos] = true;
        }

        // set up the board
        _gameObj.set("subs", subs);
        _gameObj.set("board", board);
        _gameObj.set("scores", scores);

        // start things going
        _gameObj.startTicker("tick", 100);
    }

    /** The game object. */
    protected var _gameObj :EZGame;
}
}
