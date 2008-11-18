//
// $Id: $

package com.threerings.msoy.avrg.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;

import com.whirled.game.client.PropertySpaceHelper;

import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;

public class PropertySpaceObjectImpl extends SimpleStreamableObject
    implements PropertySpaceObject
{
    public function getUserProps () :Object
    {
        return _props;
    }

    public function getPropService () :PropertySpaceMarshaller
    {
        throw new Error("not used");
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        // read user properties
        PropertySpaceHelper.readProperties(this, ins);
    }

    /** The raw properties set by the game. */
    protected var _props :Object = new Object();
}

}
