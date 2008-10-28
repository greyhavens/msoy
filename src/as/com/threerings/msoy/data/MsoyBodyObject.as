//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.crowd.data.BodyObject;

/**
 * The base class for msoy bodies.
 */
public class MsoyBodyObject extends BodyObject
{
    /** Constant value for {@link #status}. */
    public static const AWAY :int = 3;

    /** The current state of the body's actor, or null if unset/unknown/default. */
    public var actorState :String;

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

    // from BodyObject
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        actorState = (ins.readField(String) as String);
    }
}
}
