package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.presents.dobj.DObject;
import com.whirled.game.client.PropertySpaceHelper;
import com.whirled.game.data.PropertySpaceObject;

/**
 * Provides a property space for a room.
 */
public class RoomPropertiesObject extends DObject
    implements PropertySpaceObject
{
    // from PropertySpaceObject
    public function getUserProps () :Object
    {
        return _props;
    }

    /** @inheritDocs */
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        PropertySpaceHelper.readProperties(this, ins);
    }

    /**
     * The current state of game data for a room.
     * On the server, this will be a byte[] for normal properties and a byte[][] for array
     * properties. On the client, the actual values are kept whole.
     */
    protected var _props :Object = new Object();
}
}
