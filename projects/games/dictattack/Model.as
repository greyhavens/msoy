//
// $Id$

package {

import com.whirled.WhirledGameControl;

/**
 * Models and manages the (distributed) state of the board. We model the state of the board as a
 * one dimensional array of letters in row major order.
 */
public class Model
{
    /** The name of the board data distributed value. */
    public static const BOARD_DATA :String = "boardData";

    public function Model (size :int, control :WhirledGameControl)
    {
        _size = size;
        _control = control;
    }

    /**
     * Called when a round starts.
     */
    public function roundDidStart () :void
    {
        // if we are in control, create a board and publish it
        if (_control.amInControl()) {
            _control.getDictionaryLetterSet(
                Content.LOCALE, _size*_size, function (letters :Array) :void {
                _control.set(BOARD_DATA, letters);
            });
        }
    }

    /**
     * Called when the round ends.
     */
    public function roundDidEnd () :void
    {
    }

    /**
     * Called by the display when the player submits a word.
     */
    public function submitWord (board :Board, word :String) :Boolean
    {
        var used :Array = new Array();

        // make sure this word is on the board and determine the columns used by this word in the
        // process
        for (var ii :int = 0; ii < word.length; ii++) {
            var c :String = word.charAt(ii);
            var idx :int = locateLetter(c, used);
            if (idx == -1) {
                // TODO: play a sound indicating the mismatch
                board.resetLetters(used);
                return false;
            }
            used.push(idx);
            board.getLetter(idx).setHighlighted(true);
        }

        // submit the word to the server to see if it is valid
        _control.checkDictionaryWord(
            Content.LOCALE, word, function (word :String, isValid :Boolean) : void {
            if (isValid) {
                // TODO: can we do this as one event?
                for (var ii :int = 0; ii < used.length; ii++) {
                    // map our local coordinates back to a global position coordinates
                    var xx :int = int(used[ii] % _size);
                    var yy :int = int(used[ii] / _size);
                    _control.set(Model.BOARD_DATA, null, getPosition(xx, yy));
                }
            } else {
                // TODO: play a sound indicating the mismatch
                board.resetLetters(used);
            }
        });

        // the word is on the board at least, so tell the caller to clear the input field
        return true;
    }

    public function getPosition (xx :int, yy :int) :int
    {
        var pos :int;
        // map the coordinates based on our player index
        switch (_control.seating.getMyPosition()) {
        case 0: pos = yy * _size + xx; break;
        case 1: pos = (_size-1 - yy) * _size + (_size-1 - xx); break;
        case 2: pos = (_size-1 - xx) * _size + yy; break; 
        case 3: pos = (_size-1 - xx) * _size + (_size-1 - yy); break; 
        }
        return pos;
    }

    public function getLetter (xx :int, yy :int) :String
    {
        var data :Array = (_control.get(BOARD_DATA) as Array);
        return data[getPosition(xx, yy)];
    }

    /**
     * Locates the column that contains the supplied letter, ignoring columns in the supplied
     * "used" columns array. The columns are searched from the center outwards. Returns the
     * position in the letters array of the matched letter or -1 if the letter could not be found.
     */
    protected function locateLetter (c :String, used :Array) :int
    {
        for (var ii :int = 0; ii < _size; ii++) {
            // this searches like so: 14 12 10 8 6 4 2 0 1 3 5 7 9 11 13
            var xx :int = int(_size/2) + ((ii%2 == 0) ? int(-ii/2) : (int(ii/2)+1));
            if (used.indexOf(xx) != -1) {
                continue; // skip already used columns
            }
            // scan from the bottom upwards looking for the first letter
            for (var yy :int = _size-1; yy >= 0; yy--) {
                var l :String = getLetter(xx, yy);
                var pos :int = yy * _size + xx;
                if (l == null) {
                    continue;
                } else if (used.indexOf(pos) != -1) {
                    break; // try the next column
                } else if (l == c) {
                    return pos;
                } else {
                    break; // try the next column
                }
            }
        }
        return -1;
    }

    protected var _size :int;
    protected var _control :WhirledGameControl;
}

}
