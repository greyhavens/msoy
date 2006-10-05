package {

import flash.display.Sprite;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

[SWF(width="480", height="480")]
public class AlphaBout extends Sprite
    implements Game, PropertyChangedListener, StateChangedListener,
               MessageReceivedListener
{
    public function AlphaBout ()
    {
        // all we have to do is add the players display, it will
        // work automatically
        var players :AlphaBoutPlayersDisplay = new AlphaBoutPlayersDisplay();
        // position it to the right of the play board
        players.x = Piece.SIZE * BOARD_SIZE + 10;
        players.y = 0;
        // TODO add me some buttons here for I am done and a button
        // that only appears if every piece is in a word for NEXT!
        // which should also be mapped to the space bar
        addChild(players);
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObject = gameObj;
    }

    /**
     * Draw the board.
     */
    protected function drawBoard () :void
    {
        // draw the board
        var max :int = BOARD_SIZE * Piece.SIZE;
        graphics.clear();
        graphics.beginFill(0xD5AA7D);
        graphics.drawRect(0, 0, max, max);
        graphics.endFill();

        graphics.lineStyle(1.2);
        for (var ii :int = 0; ii <= BOARD_SIZE; ii++) {
            var d :int = (ii * Piece.SIZE);
            graphics.moveTo(0, d);
            graphics.lineTo(max, d);

            graphics.moveTo(d, 0);
            graphics.lineTo(d, max);
        }
    }

    // add a letter to the board somewhere towards the bottom right corner
    protected function addLetter (letterIndex :int) :void
    {
        var piecesIndex :int = findBlankSquare();
        var piece :Piece = new Piece(this, piecesIndex, letterIndex);
        _pieces[piecesIndex] = letterIndex;
        piece.x = Piece.SIZE * idxToX(piecesIndex) + (Piece.SIZE / 2);
        piece.y = Piece.SIZE * idxToY(piecesIndex) + (Piece.SIZE / 2);
        addChild(piece);
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
        if (event.type == StateChangedEvent.GAME_STARTED) {
            _gameObject.localChat("Begin your Alpha Bout!");

            // if we are the first player do some setup
            if (_gameObject.getMyIndex() == 0) {
                setupPieceBag();
                dealInitialPieces(); 
                _gameObject.set("startGame", true);
            }
            _pieces = new Array(BOARD_SIZE * BOARD_SIZE);
            // initialize the board to have no letters
            for (var ii :int = 0; ii < _pieces.length; ii++) {
                _pieces[ii] = Piece.NO_LETTER;
            }
            drawBoard();

        } else if (event.type == StateChangedEvent.GAME_ENDED) {
            _gameObject.localChat("Thank you for playing AlphaBout!");

        }
    }

    // from PropertyChangedListener 
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
        var name :String = event.name;
        if (name == NEW_PIECE) {
            var valueArray :Array = event.value as Array;
            for each (var letterIndex :int in valueArray) {
                addLetter(letterIndex);
            }
        }
    }

    public function coordsToIdx (x :int, y :int) :int
    {
        return (int(((y - 16) / Piece.SIZE) * BOARD_SIZE) + 
                int((x - 16) / Piece.SIZE));
    }

    public function idxToX (index :int) :int
    {
        return (index % BOARD_SIZE);
    }

    public function idxToY (index :int) :int
    {
        return (index / BOARD_SIZE);
    }

    // deal the inital pieces to each player
    protected function dealInitialPieces () :void
    {
        var playerNames :Array = _gameObject.getPlayerNames();
        for (var ii :int = 0; ii < playerNames.length; ii++) {
            _gameObject.dealFromCollection(
                PIECE_BAG, INITIAL_PIECES, NEW_PIECE, null, ii);
        }
    }

    // populate the shared pieceBag
    protected function setupPieceBag () :void
    {
        var bag :Array = new Array();
        var ii :int = 0;
        for each (var idx :int in LETTER_DISTRIBUTION) {
            for (var jj :int = 1; jj <= LETTER_DISTRIBUTION[idx]; jj++) {
                bag[ii] = idx;
                ii++;
            }
        }
        _gameObject.setCollection(PIECE_BAG, bag);
    }

    // find a square in _pieces towards the bottom that is empty
    protected function findBlankSquare () :int
    {
        var ii :int;
        for (ii = (BOARD_SIZE * BOARD_SIZE) - 1; ii > 0; ii--) {
            if (_pieces[ii] == Piece.NO_LETTER) {
                return ii;
            }
        }
        return -1;
    }

    // called from the letter sprite when a piece is dropped
    // pieceIndex is the old index. x, y is the new position of the piece
    // returns the new index. Returns -1 if there is already a piece there
    public function letterMoved (oldIndex :int, x :int, y :int) :int {
        var newIndex :int = coordsToIdx(x, y);
        if (_pieces[newIndex] != Piece.NO_LETTER) {
            return -1;
        }
        _pieces[newIndex] = _pieces[oldIndex];
        _pieces[oldIndex] = Piece.NO_LETTER;
        return newIndex;
    }

    protected var _pieces :Array;

    protected static const BOARD_SIZE :int = 15;

    protected static const INITIAL_PIECES :int = 7;

    protected static const PIECE_BAG :String = "pieceBag";

    protected static const NEW_PIECE :String = "newPiece";

    // Letter distribution using the patented $crabble system for now
    protected static const LETTER_DISTRIBUTION :Array = new Array(
        /* blank spot for now */ 0, /* A's */ 9, /* B's */ 2,
        /* C's */ 2, /* D's */ 4, /* E's */ 12, /* F's */ 2,
        /* G's */ 3, /* H's */ 2, /* I's */ 9, /* J's */ 1,
        /* K's */ 1, /* L's */ 4, /* M's */ 2, /* N's */ 6,
        /* O's */ 8, /* P's */ 2, /* Q's */ 1, /* R's */ 6,
        /* S's */ 4, /* T's */ 6, /* U's */ 4, /* V's */ 2,
        /* W's */ 2, /* X's */ 1, /* Y's */ 2, /* Z's */ 1)

    /** Our game object. */
    protected var _gameObject :EZGame;
}
}
