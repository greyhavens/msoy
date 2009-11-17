//
// $Id$

package com.threerings.msoy.tutorial.client {

import com.threerings.util.Predicates;

/**
 * Helper class defining some standard user level ranges and methods to check against them.
 */
internal class Levels
{
    /** Levels for very new users. */
    public static const NEWBIE :Levels = new Levels(1, 1);

    /** Levels for beginner users. */
    public static const BEGINNER :Levels = new Levels(1, 15);

    /** Levels for intermediate users. */
    public static const INTERMEDIATE :Levels = new Levels(10, 25);

    /** Levels for advanced users. */
    public static const ADVANCED :Levels = new Levels(20, int.MAX_VALUE);

    /** The minimum level at which a user is considered in this range. */
    public var min :int;

    /** The maximum level at which a user is considered in this range. */
    public var max :int;

    /**
     * Creates a predicate that combines an optional level check with an optional other previously
     * created preficate function.
     */
    public static function makeCheck (
        levels :Levels, getMemberLevel :Function, check :Function) :Function
    {
        if (levels == null) {
            return check;
        }

        function levelOkay () :Boolean {
            var level :int = getMemberLevel();
            return level >= levels.min && level <= levels.max;
        }

        return check == null ? levelOkay : Predicates.createAnd(check, levelOkay);
    }

    /** @private */
    public function Levels (min :int, max :int)
    {
        this.min = min;
        this.max = max;
    }
}
}
