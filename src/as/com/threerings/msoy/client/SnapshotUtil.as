//
// $Id$

package com.threerings.msoy.client {

import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;

import flash.geom.Matrix;

import com.threerings.util.Log;

public class SnapshotUtil
{
    /**
     * Utility for use by Snapshottable containers to snapshot their children.
     */
    public static function snapshot (
        container :DisplayObjectContainer, bitmapData :BitmapData, matrix :Matrix,
        childPredicate :Function = null) :Boolean
    {
        var allSuccess :Boolean = true;
        const nn :int = container.numChildren;
        for (var ii :int = 0; ii < nn; ii++) {
            try {
                var disp :DisplayObject = container.getChildAt(ii);
                if (childPredicate != null && !childPredicate(disp)) {
                    continue;
                }

                var m :Matrix = disp.transform.matrix; // clone the child matrix
                m.concat(matrix);
                if (disp is Snapshottable) {
                    if (!Snapshottable(disp).snapshot(bitmapData, m, childPredicate)) {
                        allSuccess = false;
                    }

                } else {
                    bitmapData.draw(disp, m, null, null, null, true); // attempt!
                }

            } catch (err :SecurityError) {
                Log.getLog(SnapshotUtil).warning("Unable to snapshot child", err);
                allSuccess = false;
            }
        }
        return allSuccess;
    }
}
}
