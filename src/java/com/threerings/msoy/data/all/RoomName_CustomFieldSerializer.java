//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for {@link RoomName}.
 */
public final class RoomName_CustomFieldSerializer
{
    public static void serialize (SerializationStreamWriter streamWriter, RoomName name)
        throws SerializationException
    {
        streamWriter.writeString(name.toString());
        streamWriter.writeInt(name.getSceneId());
    }

    public static RoomName instantiate (SerializationStreamReader streamReader)
        throws SerializationException
    {        
        return new RoomName(streamReader.readString(), streamReader.readInt());
    }

    public static void deserialize (SerializationStreamReader streamReader, RoomName instance)
    {
    }
}
