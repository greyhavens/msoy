//
// $Id$

package dictattack {

import com.whirled.WhirledGameControl;

/**
 * Models and manages the (distributed) state of the board. We model the state of the board as a
 * one dimensional array of letters in row major order.
 */
public class Model
{
    /** The name of the board data distributed value. */
    public static const BOARD_DATA :String = "boardData";

    /** The scores for each player. */
    public static const SCORES :String = "scores";

    /** An event sent when a word is played. */
    public static const WORD_PLAY :String = "wordPlay";

    public function Model (size :int, control :WhirledGameControl)
    {
        _size = size;
        _control = control;
    }

    /**
     * Returns the type of tile at the specified coordinate.
     */
    public function getType (xx :int, yy :int) :int
    {
        var half :int = int(_size/2), quarter :int = int(_size/4);
        if (xx == half && yy == half) {
            return TYPE_TRIPLE;
        } else if ((xx == quarter || xx == (half + quarter)) &&
                   (yy == quarter || yy == (half + quarter))) {
            return TYPE_DOUBLE;
        } else {
            return TYPE_NORMAL;
        }
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
        // grant ourselves flow based on how many players we defeated
        var scores :Array = (_control.get(Model.SCORES) as Array);
        var myidx :int = _control.seating.getMyPosition();
        var beat :int = 0;
        for (var ii :int = 0; ii < scores.length; ii++) {
            if (ii != myidx && scores[ii] < scores[myidx]) {
                beat++;
            }
        }
        var factor :Number = ((0.5/3) * beat + 0.5);
        var award: int = int(factor * _control.getAvailableFlow());
        trace("Defeated: " + beat + " factor: " + factor + " award: " + award);
        if (award > 0) {
            _control.awardFlow(award);
        }
    }

    /**
     * Called by the display when the player submits a word.
     */
    public function submitWord (board :Board, word :String) :Boolean
    {
        if (word.length < MIN_WORD_LENGTH) {
            return false;
        }

        // make sure this word is on the board and determine the columns used by this word in the
        // process
        var used :Array = new Array();
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
            if (!isValid) {
                // TODO: play a sound indicating the mismatch
                board.resetLetters(used);
                return;
            }

            // remove our tiles from the distributed state (we do this in individual events so that
            // watchers coming into a game half way through will see valid state), while we're at
            // it, compute our score
            var score :int = used.length - MIN_WORD_LENGTH;
            var ii :int, mult :int = 1;
            for (ii = 0; ii < used.length; ii++) {
                // map our local coordinates back to a global position coordinates
                var xx :int = int(used[ii] % _size);
                var yy :int = int(used[ii] / _size);
                mult = Math.max(TYPE_MULTIPLIER[getType(xx, yy)], mult);
                _control.set(Model.BOARD_DATA, null, getPosition(xx, yy));
            }
            // TODO: report multiplier
            score *= mult;

            // broadcast our our played word as a message
            _control.sendMessage(WORD_PLAY, used);

            // update our score
            var myidx :int = _control.seating.getMyPosition();
            var scores :Array = (_control.get(Model.SCORES) as Array);
            var newscore :int = scores[myidx] + score;
            if (score > 0) {
                _control.set(Model.SCORES, newscore, myidx);
            }

            // if we have not exceeded the winning score, stop here, otherwise end the game
            if (newscore < WINNING_SCORE) {
                return;
            }

            // end the game if our word contained the central letter
            var highest: int = 0;
            for (ii = 0; ii < scores.length; ii++) {
                if (scores[ii] > highest) {
                    highest = scores[ii];
                }
            }
            var winners :Array = new Array();
            for (ii = 0; ii < scores.length; ii++) {
                if (scores[ii] == highest) {
                    winners.push(_control.seating.getPlayerIds()[ii]);
                }
            }
            _control.endGame(winners);
        });

        // the word is on the board at least, so tell the caller to clear the input field
        return true;
    }

    public function updatePlayable (board :Board) :void
    {
        for (var xx :int = 0; xx < _size; xx++) {
            // scan from the bottom upwards looking for the first letter
            for (var yy :int = _size-1; yy >= 0; yy--) {
                var l :String = getLetter(xx, yy);
                if (l != null) {
                    board.getLetter(yy * _size + xx).setPlayable(true);
                    break;
                }
            }
        }
    }

    public function getPosition (xx :int, yy :int) :int
    {
        var pos :int;
        // map the coordinates based on our player index
        switch (_control.seating.getMyPosition()) {
        case 0: pos = yy * _size + xx; break;
        case 1: pos = (_size-1 - yy) * _size + (_size-1 - xx); break;
        case 2: pos = xx * _size + (_size-1 - yy); break; 
        case 3: pos = (_size-1 - xx) * _size + yy; break; 
        }
        return pos;
    }

    public function getReversePosition (xx :int, yy :int) :int
    {
        var pos :int;
        // map the coordinates based on our player index
        switch (_control.seating.getMyPosition()) {
        case 0: pos = yy * _size + xx; break;
        case 1: pos = (_size-1 - yy) * _size + (_size-1 - xx); break;
        case 2: pos = (_size-1 - xx) * _size + yy; break; 
        case 3: pos = xx * _size + (_size-1 - yy); break; 
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

    // TODO: get from game config
    protected static const MIN_WORD_LENGTH :int = 4;

    // TODO: get from game config
    protected static const WINNING_SCORE :int = 15;

    protected static const TYPE_NORMAL :int = 0;
    protected static const TYPE_DOUBLE :int = 1;
    protected static const TYPE_TRIPLE :int = 2;

    protected static const TYPE_MULTIPLIER :Array = [ 1, 2, 3 ];
}

}
