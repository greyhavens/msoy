//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for {@link ChannelName}.
 */
public final class ChannelName_CustomFieldSerializer
{
    public static void serialize (SerializationStreamWriter streamWriter, ChannelName name)
        throws SerializationException
    {
        streamWriter.writeString(name.toString());
        streamWriter.writeInt(name.getCreatorId());
    }

    public static ChannelName instantiate (SerializationStreamReader streamReader)
        throws SerializationException
    {
        return new ChannelName(streamReader.readString(), streamReader.readInt());
    }

    public static void deserialize (SerializationStreamReader streamReader, ChannelName instance)
    {
    }
}
