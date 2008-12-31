//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.util.Comparable;

public class PerfRecord
    implements Comparable
{
    /** The last calculated cumulative score. */
    public var calcScore :Number;

    /** The last calculated cumulative style. */
    public var calcStyle :Number;

    /**
     * Record performance from a scoring event.
     */
    public function recordPerformance (now :Number, score :Number, style :Number) :void
    {
        var index :int = _count % BUCKETS;
        _scores[index] = score;
        _styles[index] = style;
        _stamps[index] = now;

        // track totals
        _count++;
        _totalScore += score;
        _totalStyle += style;
    }

    /**
     * Calculate the instantaneous score for the specified timestamp.
     */
    public function calculateScore (now :Number) :Number
    {
        return (calcScore = getAccumulated(now, _scores));
    }

    /**
     * Calculate the instantaneous style score for the specified timestamp.
     */
    public function calculateStyle (now :Number) :Number
    {
        return (calcStyle = getAccumulated(now, _styles));
    }

    // from Comparable
    public function compareTo (other :Object) :int
    {
        var that :PerfRecord = PerfRecord(other);
        if (this.calcScore > that.calcScore) {
            return -1;

        } else if (this.calcScore < that.calcScore) {
            return 1;

        }
        return 0;
    }

    /**
     * Helper method for calculateStyle and calculateScore.
     */
    protected function getAccumulated (now :Number, values :Array) :Number
    {
        var accum :Number = 0;
        var total :Number = 0;
        var oldest :Number = now - MAX_TIME;
        for (var ii :int = 1; ii <= BUCKETS; ii++) {
            var index :int = _count - ii;
            if (index < 0) {
                break;
            }
            index = index % BUCKETS;
            if (_stamps[index] < oldest) {
                break;
            }

            // the most recent score has a weight of 1, the one before
            // a weight of .5, the one before of .25...
            var frac :Number = 1 / ii;
            accum += values[index] * frac;
            total += frac;
        }

        return (total > 0) ? (accum / total) : accum;
    }

    /** The number of scores recorded. */
    protected var _count :int;

    /** The total accumulated score points. */
    protected var _totalScore :Number;

    /** The total accumulated style points. */
    protected var _totalStyle :Number;

    // number of previous scores to count
    protected static const BUCKETS :int = 10;
    // the maximum time a score will last
    protected static const MAX_TIME :int = 30000;

    protected var _scores :Array = new Array(BUCKETS);
    protected var _styles :Array = new Array(BUCKETS);
    protected var _stamps :Array = new Array(BUCKETS);
}
}
