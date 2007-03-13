package {

import flash.display.Sprite;
import flash.display.MovieClip;

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

[SWF(width="400", height="400")]
public class Reversi extends Sprite
    implements PropertyChangedListener, StateChangedListener
{
    public function Reversi ()
    {
        _gameCtrl = new EZGameControl(this);
        _gameCtrl.registerListener(this);

        var config :Object = _gameCtrl.getConfig();
        if ("boardSize" in config) {
            _boardSize = int(config["boardSize"]);

        } else {
            _boardSize = 8;
        }

        var players :ReversiPlayersDisplay = new ReversiPlayersDisplay();
        // position it to the right of the play board
        players.x = Piece.SIZE * _boardSize + 10;
        players.y = 0;
        addChild(players);

        players.setGameControl(_gameCtrl);
    }

    /**
     * Called to initialize the piece sprites and start the game.
     */
    protected function setUpPieces () :void
    {
        _pieces = new Array();
        var ii :int;
        for (ii = 0; ii < _boardSize * _boardSize; ii++) {
            var piece :Piece = new Piece(this, ii);
            piece.x = Piece.SIZE * _board.idxToX(ii);
            piece.y = Piece.SIZE * _board.idxToY(ii);
            addChild(piece);
            _pieces[ii] = piece;
        }

        // draw the board
        var max :int = _boardSize * Piece.SIZE;
        graphics.clear();
        graphics.beginFill(0x77FF77);
        graphics.drawRect(0, 0, max, max);
        graphics.endFill();

        graphics.lineStyle(1.2);
        for (ii = 0; ii <= _boardSize; ii++) {
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
        var myIdx :int = _gameCtrl.seating.getMyPosition();
        _board.playPiece(pieceIndex, myIdx);
        _gameCtrl.endTurn();

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
            if (_gameCtrl.get("lastMove") === ii) {
                piece.showLast(true);
            }
        }
    }

    protected function showMoves () :void
    {
        readBoard();

        var turnHolderId :int = _gameCtrl.getTurnHolder();
        var turnHolder :int = _gameCtrl.seating.getPlayerPosition(turnHolderId);
        var myTurn :Boolean = _gameCtrl.isMyTurn();

        var moves :Array = _board.getMoves(turnHolder);
        for each (var index :int in moves) {
            (_pieces[index] as Piece).setDisplay(turnHolder, true, myTurn);
        }

        // detect end-game or other situations
        if (myTurn && moves.length == 0) {
            // we cannot move, so we'll pass back to the other player
            if (_board.getMoves(1 - turnHolder).length == 0) {
                // ah, but they can't move either, so the game is over
                var winnerIndex :int = _board.getWinner();
                var winnerId :int = 0;
                for each (var playerId :int in _gameCtrl.seating.getPlayerIds()) {
                    if (_gameCtrl.seating.getPlayerPosition(playerId) == winnerIndex) {
                        winnerId = playerId;
                        break;
                    }
                }
                _gameCtrl.endGame([winnerId]);
                if (winnerId == 0) {
                    _gameCtrl.sendChat("The game was a tie!");
                } else {
                    _gameCtrl.sendChat(
                        _gameCtrl.getOccupantName(winnerId) + " has won!");
                }

            } else {
                _gameCtrl.sendChat(
                    _gameCtrl.getOccupantName(turnHolderId) +
                    " cannot play and so loses a turn.");
                _gameCtrl.endTurn();
            }
        }
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
        if (event.type == StateChangedEvent.TURN_CHANGED) {
            if (_pieces == null) {
                // if we're the first player, we take care of setting up the
                // board
                if (_gameCtrl.isMyTurn()) {
                    _board.initialize();
                    _gameCtrl.set("startGame", true);
                    setUpPieces();
                }

            } else {
                showMoves();
            }

        } else if (event.type == StateChangedEvent.GAME_STARTED) {
            _gameCtrl.localChat("Reversi superchallenge: go!");

            // configure the board
            _board = new Board(_gameCtrl, _boardSize);

        } else if (event.type == StateChangedEvent.GAME_ENDED) {
            _gameCtrl.localChat("Thank you for playing Reversi!");

        }
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
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

    protected var _pieces :Array;

    protected var _boardSize :int;

    protected var _board :Board;

    /** Our game control object. */
    protected var _gameCtrl :EZGameControl;
}
}
