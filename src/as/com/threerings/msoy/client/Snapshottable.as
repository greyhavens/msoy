//
// $Id$

package com.threerings.msoy.client {

import flash.display.BitmapData;

import flash.geom.Matrix;

/**
 * An interface implemented by things that know how to take a snapshot of themselves.
 */
public interface Snapshottable
{
    /**
     * Snapshot this element into the specified BitmapData.
     * @param bitmapData the bitmap into which this thing should be snapshotted.
     * @param matrix the current transform matrix.
     * @param childPredicate if non-null, returns false for children to exclude.
     *
     * @return true if no security errors were encountered.
     */
    function snapshot (bitmapData :BitmapData, matrix :Matrix, childPredicate :Function = null)
        :Boolean;
}
}
