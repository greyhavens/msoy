//
// $Id$

package com.threerings.msoy.data {

import com.threerings.crowd.data.BodyObject;

/**
 * The base class for msoy bodies.
 */
public class MsoyBodyObject extends BodyObject
{
    /** Constant value for {@link #status}. */
    public static const AWAY :int = 3;

    // from BodyObject
    override protected function getStatusTranslation () :String
    {
        switch (status) {
        case AWAY:
            return "away";

        default:
            return super.getStatusTranslation();
        }
    }
}
}
