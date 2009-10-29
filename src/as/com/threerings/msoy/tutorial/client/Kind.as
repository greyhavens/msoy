//
// $Id$

package com.threerings.msoy.tutorial.client {

import com.threerings.util.Enum;

/**
 * Kinds of tutorial items. Only used internally by the tutorial director.
 */
internal class Kind extends Enum
{
    /** A suggestion, normally shown when a certain event occurs. */
    public static const SUGGESTION :Kind = new Kind("SUGGESTION");

    /** A tip, shown when nothing much has happened for a while. */
    public static const TIP :Kind = new Kind("TIP");

    /** A promotion, shown when nothing much has happened and cannot be ignored. */
    public static const PROMOTION :Kind = new Kind("PROMOTION");

    finishedEnumerating(Kind);

    /** @private */
    public function Kind (name :String)
    {
        super(name);
    }

    // omitting valueOf and values since we don't require them
}
}
