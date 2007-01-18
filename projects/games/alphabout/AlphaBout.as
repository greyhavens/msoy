package {

import flash.display.Sprite;
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

[SWF(width="530", height="530")]
public class AlphaBout extends Sprite
    implements PropertyChangedListener, StateChangedListener, MessageReceivedListener
{
    // theme constants
    public static const NUMBER_OF_THEMES :int = 4;
    public static const BASIC_THEME :int = 0;
    public static const TIMES_THEME :int = 1;
    public static const RANSOM_THEME :int = 2;
    public static const PHOTO_THEME :int = 3;

    public function AlphaBout ()
    {
        // all we have to do is add the players display, it will
        // work automatically
        var players :AlphaBoutPlayersDisplay = new AlphaBoutPlayersDisplay();
        // add our board
        _board = new Board(this);
        addChild(_board);
        // setup our dictionary
        _dict = new Dict();
        _dict.init();
        // position it to the right of the play board
        players.x = Piece.SIZE * _board.getSize() + 10;
        players.y = 0;
        // TODO add me some buttons here for I am done and a button
        // that only appears if every piece is in a word for NEXT!
        // which should also be mapped to the space bar
        addChild(players);

        _gameCtrl = new EZGameControl(this);
        players.setGameControl(_gameCtrl);

        if (_gameCtrl.getMyIndex() != -1) {
            _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, keyDownHandler);
            _gameCtrl.registerListener(this);
        }
    }

    // called from the letter sprite when a piece is dropped
    // pieceIndex is the old index. x, y is the new position of the piece
    // returns the new index. Returns -1 if there is already a piece there
    public function letterMoved (oldIndex :int, x :int, y :int) :int {
        if (!validXandY(x,y)) {
            return -1;
        }
        var newIndex :int = coordsToIdx(x, y);
        if (!_pieces[newIndex].hasNoLetter()) {
            return -1;
        }
        var tmpPiece :Piece = Piece(_pieces[newIndex]);
        _pieces[newIndex] = _pieces[oldIndex];
        _pieces[oldIndex] = tmpPiece;

        // our board has changed so highlight the words
        highlightWords();
        
        return newIndex;
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
        if (event.type == StateChangedEvent.GAME_STARTED) {
            _gameCtrl.localChat("Begin your Alpha Bout!\n");

            // if we are the first player do some setup
            if (_gameCtrl.getMyIndex() == 0) {
                setupPieceBag();
                dealPlayersPieces(INITIAL_PIECES);
                _gameCtrl.set("startGame", true);
            }
            _pieces = new Array(_board.getSize() * _board.getSize());
            // initialize the board to have no letters
            for (var ii :int = 0; ii < _pieces.length; ii++) {
               _pieces[ii] = new Piece(this, ii, Piece.NO_LETTER);
            }
        } else if (event.type == StateChangedEvent.GAME_ENDED) {
            _gameCtrl.localChat("Thank you for playing AlphaBout!\n");
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
            // TODO if I can't get this to work, then just store how many
            // pieces I should be getting at the start and check that.
            /*
            if (valueArray.length == 0) {
                _gameCtrl.endGame(_gameObject.getMyIndex());
            }
            */
            for each (var letterIndex :int in valueArray) {
                addPiece(letterIndex);
            }
        }
    }

    public function coordsToIdx (x :int, y :int) :int
    {
        // TODO clean up this math
        return (int(y / Piece.SIZE) * _board.getSize()) +
                int(x / Piece.SIZE) - Piece.SIZE;
    }

    public function idxToX (index :int) :int
    {
        return (Piece.SIZE * int(index % _board.getSize())) +
                getBoardBorder() + int(Piece.SIZE / 2);
    }

    public function idxToY (index :int) :int
    {
        return (Piece.SIZE * int(index / _board.getSize())) +
                getBoardBorder() + int(Piece.SIZE / 2);
    }

    public function getTheme () :int
    {
        return _theme;
    }

    public function getBoardBorder () :int
    {
        return _board.getBorder();
    }

    protected function keyDownHandler (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
          case Keyboard.RIGHT:
            if (_theme < (NUMBER_OF_THEMES - 1)) {
                _theme++;
            } else {
                _theme = 0;
            }
            themeChange();
            break;
          case Keyboard.LEFT:
            if (_theme > 0) {
                _theme--;
            } else {
                _theme = (NUMBER_OF_THEMES - 1);
            }
            themeChange();
            break;
          case Keyboard.SPACE:
            // TODO if bag is empty send the end of game event
            if (_boardComplete) {
                dealPlayersPieces();
            } else {
                _gameCtrl.localChat("Your board is not complete.\n");
            }
            break;
        }
    }

    // tell all of the pieces to update their theme
    protected function changePiecesTheme() :void
    {
        for (var ii :int = 0; ii < _pieces.length; ii++) {
            if (!_pieces[ii].hasNoLetter()) {
                _pieces[ii].updateTheme();
            }
        } 
    }

    // add a letter to the board somewhere towards the bottom right corner
    // optionally pass the index in _pieces where this piece is currently
    protected function addPiece (letterIndex :int, piecesIndex :int = -1) :void
    {
        if (piecesIndex == -1) {
            piecesIndex = findBlankSquare();
        }
        var piece :Piece = new Piece(this, piecesIndex, letterIndex);
        _pieces[piecesIndex] = piece;
        piece.x = idxToX(piecesIndex);
        piece.y = idxToY(piecesIndex);
        addChild(piece);
    }

    // TODO Since board size/border and piece size could potentially change 
    // when a theme changes, there should be some code in here that adjusts 
    // the pieces x and y based on the new board values.
    protected function themeChange () :void
    {
        _board.changeTheme();
        changePiecesTheme();
    }

    // deal pieces to players. by default, only one
    protected function dealPlayersPieces (count :int = 1) :void
    {
        var playerNames :Array = _gameCtrl.getPlayerNames();
        for (var ii :int = 0; ii < playerNames.length; ii++) {
            _gameCtrl.dealFromCollection(
                PIECE_BAG, count, NEW_PIECE, null, ii);
        }
    }

    // populate the shared pieceBag
    protected function setupPieceBag () :void
    {
        // TODO this needs to find out how many players are playing, and only
        // add enough pieces so everyone can get one.
        var bag :Array = new Array();
        var ii :int = 0;
        for (var idx :int = 0; idx < LETTER_DISTRIBUTION.length; idx++) {
            for (var jj :int = 1; jj <= LETTER_DISTRIBUTION[idx]; jj++) {
                bag[ii] = idx;
                ii++;
            }
        }
        _gameCtrl.setCollection(PIECE_BAG, bag);
    }

    // find a square in _pieces towards the bottom that is empty
    protected function findBlankSquare () :int
    {
        for (var ii :int = _pieces.length - 1; ii >= 0; ii--) {
            if (_pieces[ii].hasNoLetter()) {
                return ii;
            }
        }
        return -1;
    }

    protected function highlightWords () :void
    {
        // start with all words assumed invalid
        makePiecesInvalid(_pieces);

        // array to hold our word pieces
        var word :Array = new Array();
        var boardSize :int = _board.getSize();
        var piece :Piece = null;
        _currentScore = 0;
        _boardComplete = true;

        // first search all the words in rows
        for (var ii :int = 0; ii < _pieces.length; ii++) {
            piece = _pieces[ii];
            // if we have no letter or are at the edge of the board
            if (piece.hasNoLetter() || ((ii + 1) % boardSize) == 0) {
                if (!piece.hasNoLetter()) {
                    word.push(piece);
                }
                // if we previously have a word, check it
                if (word.length > 0) {
                    if (lookupWord(word)) {
                        _currentScore += wordScore(word);
                    } else {
                        // TODO if single letter is part of valid Y direction word,
                        // this should not be set
                        _boardComplete = false;
                    }
                }
                // need to clear the word out
                word = new Array();
            } else {
                // else we have a letter, add the piece to our word
                word.push(piece);
            }
        }
        
        // TODO factor out the shared loop contents into function
        // second search all the words in columns
        var column :int = 0;
        var row :int = 0;
        var yIdx :int = 0;
        word = new Array();
        for (ii = 0; ii < _pieces.length; ii++) {
            row = ((ii * boardSize) % (boardSize * boardSize));
            if (ii > 0 && row == 0) {
                column++;
            }
            yIdx = row + column;
            piece = _pieces[yIdx];
            // if we have no letter or are at the edge of the board
            if (piece.hasNoLetter() ||
                int(yIdx / boardSize) == (boardSize - 1)) {
                if (!piece.hasNoLetter()) {
                    word.push(piece);
                }
                // if we previously have a word, check it
                if (word.length > 0) {
                    if (lookupWord(word)) {
                        _currentScore += wordScore(word);
                    } else {
                        // TODO if single letter is part of valid X direction word,
                        // this should not be set
                        _boardComplete = false;
                    }
                }
                // need to clear the word out
                word = new Array();
            } else {
                // else we have a letter, add the piece to our word
                word.push(piece);
            }
        }

      _gameCtrl.localChat("Current score: " + _currentScore + "\n");
     // TODO potentially set a value that says whether the whole board has been
     // completed.. maybe just return true or false from this method 
    }

    protected function lookupWord (pieces :Array) :Boolean
    {
        var word :String = piecesToString(pieces);
        // words not in the dictionary or only one letter are invalid
        if (!_dict.contains(word)) {
            return false;
        }
        makePiecesValid(pieces);
        return true;
    }

    protected function makePiecesInvalid (pieces :Array) :void
    {
        for each (var piece :Piece in pieces) {
            piece.setLetterInvalid(); 
        }
    }

    protected function makePiecesValid (pieces :Array) :void
    {
        for each (var piece :Piece in pieces) {
            piece.setLetterValid(); 
        }
    }

    protected function wordScore (pieces :Array) :int 
    {
        var score :int = 0;
        for each (var piece :Piece in pieces) {
            score += LETTER_SCORES[piece.getLetterIndex()]; 
        }
        return score;
    }

    protected function piecesToString (pieces :Array) :String
    {
        var str :String = new String();
        for each (var piece :Piece in pieces) {
            str += piece.getLetter();
        }
        return str;
    }

    // max sure x and y are places we would even want to allow a drop
    protected function validXandY (x :int, y :int) :Boolean 
    {
        var max :int = (Piece.SIZE * _board.getSize()) + getBoardBorder();
        if (x > max || y > max) {
            return false;
        }
        var border :int = getBoardBorder();
        if (x < border || y < border) {
            return false;
        }
        return true;
    }

    protected var _pieces :Array;

    protected var _board :Board;

    protected var _dict :Dict;

    protected var _boardComplete :Boolean;

    protected static const INITIAL_PIECES :int = 7;

    protected static const PIECE_BAG :String = "pieceBag";

    protected static const NEW_PIECE :String = "newPiece";

    // our current theme
    protected var _theme :int = BASIC_THEME;

    // our current score in the game
    protected var _currentScore :int = 0;

    // Letter distribution using the patented $crabble system for now
    protected static const LETTER_DISTRIBUTION :Array = new Array(
        /* blank spot for now */ 0, /* A's */ 9, /* B's */ 2,
        /* C's */ 2, /* D's */ 4, /* E's */ 12, /* F's */ 2,
        /* G's */ 3, /* H's */ 2, /* I's */ 9, /* J's */ 1,
        /* K's */ 1, /* L's */ 4, /* M's */ 2, /* N's */ 6,
        /* O's */ 8, /* P's */ 2, /* Q's */ 1, /* R's */ 6,
        /* S's */ 4, /* T's */ 6, /* U's */ 4, /* V's */ 2,
        /* W's */ 2, /* X's */ 1, /* Y's */ 2, /* Z's */ 1)

    // Scoring based on $crabble
    protected static const LETTER_SCORES :Array = new Array(
        /* blank spot for now */ 0, /* A's */ 1, /* B's */ 3,
        /* C's */ 3, /* D's */ 2, /* E's */ 1, /* F's */ 4,
        /* G's */ 2, /* H's */ 4, /* I's */ 1, /* J's */ 8,
        /* K's */ 5, /* L's */ 1, /* M's */ 3, /* N's */ 1,
        /* O's */ 1, /* P's */ 3, /* Q's */ 10, /* R's */ 1,
        /* S's */ 1, /* T's */ 1, /* U's */ 1, /* V's */ 4,
        /* W's */ 4, /* X's */ 8, /* Y's */ 4, /* Z's */ 10)

    /** Our game object. */
    protected var _gameCtrl :EZGameControl;
}
}
