package {

import flash.display.Sprite;
import flash.display.MovieClip;

import com.metasoy.game.Game;
import com.metasoy.game.GameObject;
import com.metasoy.game.MessageReceivedEvent;
import com.metasoy.game.PlayersDisplay;
import com.metasoy.game.PropertyChangedEvent;
import com.metasoy.game.StateChangedEvent;

[SWF(width="400", height="400")]
public class Reversi extends Sprite
    implements Game
{
    public function Reversi ()
    {
        // all we have to do is add the players display, it will
        // work automatically
        var players :PlayersDisplay = new PlayersDisplay();
        // position it to the right of the play board
        players.x = Piece.SIZE * BOARD_SIZE + 10;
        players.y = 0;
        addChild(players);
    }

    // from Game
    public function setGameObject (gameObj :GameObject) :void
    {
        _gameObject = gameObj;
        _gameObject.addEventListener(PropertyChangedEvent.TYPE, propChanged);
        _gameObject.addEventListener(MessageReceivedEvent.TYPE, msgReceived);
        _gameObject.addEventListener(
            StateChangedEvent.GAME_STARTED, gameStarted);
        _gameObject.addEventListener(
            StateChangedEvent.GAME_ENDED, gameEnded);
        _gameObject.addEventListener(
            StateChangedEvent.TURN_CHANGED, turnChanged);
    }

    /**
     * Called to initialize the piece sprites and start the game.
     */
    protected function setUpPieces () :void
    {
        _pieces = new Array();
        var ii :int;
        for (ii = 0; ii < BOARD_SIZE * BOARD_SIZE; ii++) {
            var piece :Piece = new Piece(this, ii);
            piece.x = Piece.SIZE * _board.idxToX(ii);
            piece.y = Piece.SIZE * _board.idxToY(ii);
            addChild(piece);
            _pieces[ii] = piece;
        }

        // draw the board
        var max :int = BOARD_SIZE * Piece.SIZE;
        graphics.clear();
        graphics.beginFill(0x77FF77);
        graphics.drawRect(0, 0, max, max);
        graphics.endFill();

        graphics.lineStyle(1.2);
        for (ii = 0; ii <= BOARD_SIZE; ii++) {
            var d :int = (ii * Piece.SIZE);
            graphics.moveTo(0, d);
            graphics.lineTo(max, d);

            graphics.moveTo(d, 0);
            graphics.lineTo(d, max);
        }

        showMoves();
    }

    public function pieceClicked (pieceIndex :int) :void
    {
        // enact the play
        var myIdx :int = _gameObject.getMyIndex();
        _board.playPiece(pieceIndex, myIdx);
        _gameObject.endTurn();

        // display something so that the player knows they clicked
        readBoard();
        (_pieces[pieceIndex] as Piece).showLast(true);
    }

    protected function readBoard () :void
    {
        // re-read the whole thing
        for (var ii :int = 0; ii < _pieces.length; ii++) {
            var piece :Piece = (_pieces[ii] as Piece);
            piece.setDisplay(_board.getPiece(ii));
            if (_gameObject.data["lastMove"] === ii) {
                piece.showLast(true);
            }
        }
    }

    protected function showMoves () :void
    {
        readBoard();

        var turnHolder :int = _gameObject.getTurnHolderIndex();
        var myTurn :Boolean = _gameObject.isMyTurn();

        var moves :Array = _board.getMoves(turnHolder);
        for each (var index :int in moves) {
            (_pieces[index] as Piece).setDisplay(turnHolder, true, myTurn);
        }

        // detect end-game or other situations
        if (myTurn && moves.length == 0) {
            // we cannot move, so we'll pass back to the other player
            if (_board.getMoves(1 - turnHolder).length == 0) {
                // ah, but they can't move either, so the game is over
                var winner :int = _board.getWinner();
                _gameObject.endGame(winner);
                if (winner == -1) {
                    _gameObject.sendChat("The game was a tie!");
                } else {
                    _gameObject.sendChat(
                        _gameObject.getPlayerNames()[winner] + " has won!");
                }

            } else {
                _gameObject.sendChat(
                    _gameObject.getPlayerNames()[turnHolder] +
                    " cannot play and so loses a turn.");
                _gameObject.endTurn();
            }
        }
    }

    /**
     * A callback we've registered to received MessageReceivedEvents.
     */
    protected function msgReceived (event :MessageReceivedEvent) :void
    {
        // nada
    }

    /**
     * A callback we've registered to received StateChangedEvent.GAME_STARTED.
     */
    protected function gameStarted (event :StateChangedEvent) :void
    {
        _gameObject.localChat("Reversi superchallenge: go!");

        // configure the board
        _board = new Board(_gameObject, BOARD_SIZE);
    }

    /**
     * A callback we've registered to received StateChangedEvent.GAME_STARTED.
     */
    protected function gameEnded (event :StateChangedEvent) :void
    {
        _gameObject.localChat("Thank you for playing Reversi!");
    }

    /**
     * A callback we've registered to received PropertyChangedEvents
     */
    protected function propChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        if (name == "board") {
            if (event.index != -1) {
                // read the change
                readBoard();

            } else if (_pieces == null) {
                // the other player has initialized the game
                setUpPieces();
            }
        }
    }

    /**
     * A callback we've registered to received StateChangedEvent.TURN_CHANGED.
     */
    protected function turnChanged (event :StateChangedEvent) :void
    {
        if (_pieces == null) {
            // if we're the first player, we take care of setting up the board
            if (_gameObject.isMyTurn()) {
                _board.initialize();
                _gameObject.set("startGame", true);
                setUpPieces();
            }

        } else {
            showMoves();
        }
    }

    protected var _pieces :Array;

    protected static const BOARD_SIZE :int = 4;

    protected var _board :Board;

    /** Our game object. */
    protected var _gameObject :GameObject;
}
}
