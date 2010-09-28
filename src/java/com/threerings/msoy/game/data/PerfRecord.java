//
// $Id$

package com.threerings.msoy.game.data;

import com.google.common.primitives.Floats;

public class PerfRecord
    implements Comparable<PerfRecord>
{
    /** The last calculated cumulative score. */
    public float calcScore;

    /** The last calculated cumulative style. */
    public float calcStyle;

    /**
     * Record performance from a scoring event.
     */
    public void recordPerformance (long now, float score, float style)
    {
        int index = _count % BUCKETS;
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
    public float calculateScore (long now)
    {
        return (calcScore = getAccumulated(now, _scores));
    }

    /**
     * Calculate the instantaneous style score for the specified timestamp.
     */
    public float calculateStyle (long now)
    {
        return (calcStyle = getAccumulated(now, _styles));
    }

    /**
     * Get the average score over all scores posted.
     */
    public float getAverageScore ()
    {
        return (_count == 0) ? 0 : _totalScore / _count;
    }

    /**
     * Get the average style over all styles posted.
     */
    public float getAverageStyle ()
    {
        return (_count == 0) ? 0 : _totalStyle / _count;
    }

    // from Comparable
    public int compareTo (PerfRecord that)
    {
        return Floats.compare(calcScore, that.calcScore);
    }

    /**
     * Helper method for calculateStyle and calculateScore.
     */
    protected float getAccumulated (long now, float[] values)
    {
        float accum = 0;
        float total = 0;
        long oldest = now - MAX_TIME;
        for (int ii = 1; ii <= BUCKETS; ii++) {
            int index = _count - ii;
            if (index < 0) {
                break;
            }
            index = index % BUCKETS;
            if (_stamps[index] < oldest) {
                break;
            }

            // the most recent score has a weight of 1, the one before
            // a weight of .5, the one before of .25...
            float frac = 1f / ii;
            accum += values[index] * frac;
            total += frac;
        }

        return (total > 0) ? (accum / total) : accum;
    }

    /** The number of scores recorded. */
    protected int _count;

    /** The total accumulated score points. */
    protected float _totalScore;

    /** The total accumulated style points. */
    protected float _totalStyle;

    // number of previous scores to count
    protected static final int BUCKETS = 10;
    // the maximum time a score will last
    protected static final int MAX_TIME = 30000;

    protected float[] _scores = new float[BUCKETS];
    protected float[] _styles = new float[BUCKETS];
    protected long[] _stamps = new long[BUCKETS];
}
