package {

import flash.geom.Point;

public class Board
{
    public static const NO_PIECE :int = -1;
    public static const WHITE_IDX :int = 0;
    public static const BLACK_IDX :int = 1;

    public function Board (dimension :int = 8)
    {
        _dimension = dimension;
        _data = new Array();

        if (_dimension > 0) {
            _data.length = (_dimension * _dimension);

            // configure the starting board configuration
            for (var ii :int = 0; ii < _data.length; ii++) {
                _data[ii] = NO_PIECE;
            }
            var half :int = _dimension / 2;
            setPiece(half, half, WHITE_IDX);
            setPiece(half + 1, half, BLACK_IDX);
            setPiece(half, half + 1, BLACK_IDX);
            setPiece(half + 1, half + 1, WHITE_IDX);
        }
    }

    /**
     * Get the piece at the specified spot.
     *
     * @return -1, or 0, or 1.
     */
    public function getPiece (x :int, y :int) :int
    {
        return int(_data[checkCoords(x, y)]);
    }

    /**
     * @return an array of Point objects that specify the valid move
     * coordinates for the given player.
     */
    public function getMoves (playerIdx :int) :Array
    {
        checkPlayerIdx(playerIdx);
        var moves :Array = new Array();

        // for each space, it is a legal move if 
        for (var xx :int = 0; xx < _dimension; xx++) {
            for (var yy :int = 0; yy < _dimension; yy++) {
                if (isValidMove(xx, yy, playerIdx)) {
                    moves.push(new Point(xx, yy));
                }
            }
        }
        return moves;
    }

    public function playPiece (x :int, y :int, playerIdx :int) :void
    {
        checkPlayerIdx(playerIdx);
        if (!isValidMove(x, y, playerIdx)) {
            throw new ArgumentError("Invalid move!");
        }

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
    }

    /**
     * Is the specified square a legal play for the specified player?
     */
    public function isValidMove (x :int, y :int, playerIdx :int) :Boolean
    {
        // the square must be blank
        if (getPiece(x, y) != NO_PIECE) {
            return false;
        }

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

    protected function setPiece (x :int, y :int, playerIdx :int) :void
    {
        _data[coordsToIdx(x, y)] = playerIdx;
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
            // there must be at least 1 piece of the opponent's color
            if (getPiece(x, y) != (1 - playerIdx)) {
                return false;
            }
            while (true) {
                x += dx;
                y += dy;
                var piece :int = getPiece(x, y);
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
            var piece :int = getPiece(x, y);
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

    protected function coordsToIdx (x :int, y :int) :int
    {
        return (x * _dimension + y);
    }

    /**
     * Like coordsToIdx, but validates the coordinates.
     */
    protected function checkCoords (x :int, y :int) :int
    {
        if (x < 0 || x >= _dimension || y < 0 || y >= _dimension) {
            throw new RangeError("X and Y must be in the range [0.." +
                _dimension + "].");
        }
        return coordsToIdx(x, y);
    }

    protected function checkPlayerIdx (idx :int) :void
    {
        if (idx != 0 && idx != 1) {
            throw new RangeError("Player index must be 0 or 1.");
        }
    }

    /** The length of one side of the board. */
    protected var _dimension :int;

    /** An array representing the current state of the board.
     * Each element is null, 0, or 1.
     */
    protected var _data :Array;
}
}
