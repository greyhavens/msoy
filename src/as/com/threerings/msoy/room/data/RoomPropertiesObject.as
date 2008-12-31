package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.presents.dobj.DObject;
import com.whirled.game.client.PropertySpaceHelper;
import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;
import com.whirled.game.data.WhirledGameMessageMarshaller;

/**
 * Provides a property space for a room.
 */
public class RoomPropertiesObject extends DObject
    implements PropertySpaceObject
{
    /** Name of messages posted as a result of a message service call. */
    public static const USER_MESSAGE :String = "Umsg";

    /** Service for setting the properties. */
    public var propertiesService :PropertySpaceMarshaller;

    /** Service for sending messages to the room occupants (that are also playing the game that
     * these properties belong to). */
    public var messageService :WhirledGameMessageMarshaller;

    // from PropertySpaceObject
    public function getUserProps () :Object
    {
        return _props;
    }

    // from PropertySpaceObject
    public function getPropService () :PropertySpaceMarshaller
    {
        return propertiesService;
    }

    /** @inheritDocs */
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        propertiesService = PropertySpaceMarshaller(ins.readObject());
        messageService = WhirledGameMessageMarshaller(ins.readObject());
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
