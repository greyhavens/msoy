package {

import com.threerings.ezgame.EZGameControl;

public class Board
{
    public static const NO_PIECE :int = -1;
    public static const WHITE_IDX :int = 0;
    public static const BLACK_IDX :int = 1;

    public function Board (gameCtrl :EZGameControl, lengthOfSide :int = 8)
    {
        _gameCtrl = gameCtrl;
        _lengthOfSide = lengthOfSide;
    }

    public function initialize () :void
    {
        var data :Array = new Array();
        data.length = (_lengthOfSide * _lengthOfSide);

        // blank out every piece
        for (var ii :int = 0; ii < data.length; ii++) {
            data[ii] = NO_PIECE;
        }

        // but then set up the 4 starting pieces in the middle
        var half :int = (_lengthOfSide - 1) / 2;
        data[coordsToIdx(half, half)] = WHITE_IDX;
        data[coordsToIdx(half + 1, half)] = BLACK_IDX;
        data[coordsToIdx(half, half + 1)] = BLACK_IDX;
        data[coordsToIdx(half + 1, half + 1)] = WHITE_IDX;

        // assign it to the game object
        _gameCtrl.setImmediate("board", data);
    }

    /**
     * Get the piece at the specified spot.
     *
     * @return -1, or 0, or 1.
     */
    public function getPiece (index :int) :int
    {
        checkIndex(index);
        return int(_gameCtrl.get("board")[index]);
    }

    public function getPieceByCoords (x :int, y :int) :int
    {
        return getPiece(coordsToIdx(x, y));
    }

    /**
     * @return an array of indicies that specify the valid move
     * coordinates for the given player.
     */
    public function getMoves (playerIdx :int) :Array
    {
        checkPlayerIdx(playerIdx);
        var moves :Array = new Array();

        // for each space, it is a legal move if 
        for (var ii :int = (_lengthOfSide * _lengthOfSide) - 1; ii >= 0; ii--) {
            if (isValidMove(ii, playerIdx)) {
                moves.push(ii);
            }
        }
        return moves;
    }

    public function playPiece (index :int, playerIdx :int) :void
    {
        if (!isValidMove(index, playerIdx)) {
            throw new ArgumentError("Invalid move!");
        }

        var x :int = idxToX(index);
        var y :int = idxToY(index);

        // flip any qualifying pieces of the opponent's color
        for (var dx :int = -1; dx <= 1; dx++) {
            for (var dy :int = -1; dy <= 1; dy++) {
                if (dy != 0 || dx != 0) {
                    applyMoveDirection(x, y, dx, dy, playerIdx);
                }
            }
        }

        // finally, place the new piece
        setPiece(x, y, playerIdx);
        _gameCtrl.setImmediate("lastMove", index);
    }

    /**
     * Is the specified square a legal play for the specified player?
     */
    public function isValidMove (index :int, playerIdx :int) :Boolean
    {
        checkPlayerIdx(playerIdx);
        checkIndex(index);

        // the square must be blank
        if (getPiece(index) != NO_PIECE) {
            return false;
        }

        var x :int = idxToX(index);
        var y :int = idxToY(index);

        // and at least one direction must contain opponent pieces
        // followed by one of our own
        for (var dx :int = -1; dx <= 1; dx++) {
            for (var dy :int = -1; dy <= 1; dy++) {
                if (dy != 0 || dx != 0) {
                    if (isValidMoveDirection(x, y, dx, dy, playerIdx)) {
                        return true;
                    }
                }
            }
        }

        // not a valid move
        return false;
    }

    /**
     * Determine the winner, or return -1 for a tie.
     * This should only be called after getMoves() returns an empty
     * array for both players.
     */
    public function getWinner () :int
    {
        var whiteCount :int = 0;
        var blackCount :int = 0;
        for each (var piece :int in _gameCtrl.get("board")) {
            if (piece == WHITE_IDX) {
                whiteCount++;

            } else if (piece == BLACK_IDX) {
                blackCount++;
            }
        }

        if (whiteCount > blackCount) {
            return WHITE_IDX;

        } else if (blackCount > whiteCount) {
            return BLACK_IDX;

        } else {
            return -1;
        }
    }

    protected function setPiece (x :int, y :int, playerIdx :int) :void
    {
        _gameCtrl.setImmediate("board", playerIdx, coordsToIdx(x, y));
    }

    /**
     * Return true if the direction specified contains a run of
     * pieces of the other color, followed by one of our own.
     */
    protected function isValidMoveDirection (
            x :int, y :int, dx :int, dy :int, playerIdx :int) :Boolean
    {
        try {
            x += dx;
            y += dy;
            checkCoords(x, y);
            // there must be at least 1 piece of the opponent's color
            if (getPieceByCoords(x, y) != (1 - playerIdx)) {
                return false;
            }
            while (true) {
                x += dx;
                y += dy;
                checkCoords(x, y);
                var piece :int = getPieceByCoords(x, y);
                if (piece == -1) {
                    return false;
                } else if (piece == playerIdx) {
                    return true;
                }
                // else: is opponent index, keep going
            }

        } catch (re :RangeError) {
            // fall through: if we go off the board then the whole thing fails
        }
        return false;
    }

    /**
     * Recursive function to flip pieces when a move
     */
    protected function applyMoveDirection (
            x :int, y :int, dx :int, dy :int, playerIdx :int) :Boolean
    {
        // step outward, finding if the direction will apply flips
        x += dx;
        y += dy;

        try {
            checkCoords(x, y);
            var piece :int = getPieceByCoords(x, y);
            if (piece == NO_PIECE) {
                // found a blank, so this line of inquiry is a bust
                return false;

            } else if (piece == playerIdx) {
                // found a piece of our color! We do want to flip the other
                // pieces
                return true;

            } else {
                // We found the opponent color.
                // Recurse, and flip this piece if we eventually find our color
                var result :Boolean =
                    applyMoveDirection(x, y, dx, dy, playerIdx);
                if (result) {
                    setPiece(x, y, playerIdx);
                }
                return result;
            }

        } catch (re :RangeError) {
        }
        return false;
    }

    public function coordsToIdx (x :int, y :int) :int
    {
        return (y * _lengthOfSide) + x;
    }

    public function idxToX (index :int) :int
    {
        return (index % _lengthOfSide);
    }

    public function idxToY (index :int) :int
    {
        return (index / _lengthOfSide);
    }

    /**
     * Like coordsToIdx, but validates the coordinates.
     */
    protected function checkIndex (index :int) :void
    {
        if (index < 0 || index >= (_lengthOfSide * _lengthOfSide)) {
            throw new RangeError("index must be in the range [0.." +
                (_lengthOfSide * _lengthOfSide) + "].");
        }
    }

    protected function checkCoords (x :int, y :int) :void
    {
        if (x < 0 || y < 0 || x >= _lengthOfSide || y >= _lengthOfSide) {
            throw new RangeError();
        }
    }

    protected function checkPlayerIdx (idx :int) :void
    {
        if (idx != 0 && idx != 1) {
            throw new RangeError("Player index must be 0 or 1.");
        }
    }

    /** The length of one side of the board. */
    protected var _lengthOfSide :int;

    /** An array representing the current state of the board.
     * Each element is -1, 0, or 1. */
    protected var _gameCtrl :EZGameControl;
}
}
