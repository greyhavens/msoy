package {

import flash.display.Sprite;
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;

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
        stage.addEventListener(KeyboardEvent.KEY_DOWN, keyDownHandler);
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

    // tell all of the pieces to update their theme
    protected function changePiecesTheme() :void
    {
        for (var ii :int = 0; ii < _pieces.length; ii++) {
            if (_pieces[ii].getLetterIndex() != Piece.NO_LETTER) {
                _pieces[ii].updateTheme();
            }
        } 
    }

    // add a letter to the board somewhere towards the bottom right corner
    // optionally pass the index in _pieces where this piece is currently
    protected function addLetter (letterIndex :int, piecesIndex :int = -1) :void
    {
        if (piecesIndex == -1) {
            piecesIndex = findBlankSquare();
        }
        var piece :Piece = new Piece(this, piecesIndex, letterIndex);
        _pieces[piecesIndex] = piece;
        piece.x = Piece.SIZE * idxToX(piecesIndex) + (Piece.SIZE / 2);
        piece.y = Piece.SIZE * idxToY(piecesIndex) + (Piece.SIZE / 2);
        addChild(piece);
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
        if (event.type == StateChangedEvent.GAME_STARTED) {
            _gameObject.localChat("Begin your Alpha Bout!\n");

            // if we are the first player do some setup
            if (_gameObject.getMyIndex() == 0) {
                setupPieceBag();
                dealInitialPieces(); 
                _gameObject.set("startGame", true);
            }
            _pieces = new Array(BOARD_SIZE * BOARD_SIZE);
            // initialize the board to have no letters
            for (var ii :int = 0; ii < _pieces.length; ii++) {
               _pieces[ii] = new Piece(this, ii, Piece.NO_LETTER);
            }
            drawBoard();

        } else if (event.type == StateChangedEvent.GAME_ENDED) {
            _gameObject.localChat("Thank you for playing AlphaBout!\n");

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

    protected function keyDownHandler (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
          case Keyboard.RIGHT:
            if (_theme < (NUMBER_OF_THEMES - 1)) {
                _theme++;
                themeChange();
            }
            break;
          case Keyboard.LEFT:
            if (_theme > 0) {
                _theme--;
                themeChange();
            }
            break;
        }
    }

    public function coordsToIdx (x :int, y :int) :int
    {
        return (int(((y - (Piece.SIZE / 2)) / Piece.SIZE) * BOARD_SIZE) + 
                int((x - (Piece.SIZE /2)) / Piece.SIZE));
    }

    public function idxToX (index :int) :int
    {
        return (index % BOARD_SIZE);
    }

    public function idxToY (index :int) :int
    {
        return (index / BOARD_SIZE);
    }

    public function getTheme () :int
    {
        return _theme;
    }

    protected function themeChange () :void
    {
        drawBoard();
        changePiecesTheme();
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
        for (var ii :int = _pieces.length - 1; ii >= 0; ii--) {
            if (_pieces[ii].getLetterIndex() == Piece.NO_LETTER) {
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
        if (_pieces[newIndex].getLetterIndex() != Piece.NO_LETTER) {
            return -1;
        }
        var tmpPiece :Piece = Piece(_pieces[newIndex]);
        _pieces[newIndex] = _pieces[oldIndex];
        _pieces[oldIndex] = tmpPiece;
        return newIndex;
    }

    protected var _pieces :Array;

    protected static const BOARD_SIZE :int = 15;

    protected static const INITIAL_PIECES :int = 7;

    protected static const PIECE_BAG :String = "pieceBag";

    protected static const NEW_PIECE :String = "newPiece";

    // our current theme
    protected var _theme :int = BASIC_THEME;

    // theme constants
    protected static const NUMBER_OF_THEMES :int = 3;
    protected static const BASIC_THEME :int = 0;
    protected static const TIMES_THEME :int = 1;
    protected static const RANSOM_THEME :int = 2;

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
