//
// $Id$

package com.threerings.msoy.data.gwt;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import com.threerings.msoy.data.all.ChannelName;

/**
 * Custom field serializer for {@link ChannelName}.
 */
public final class ChannelName_CustomFieldSerializer
{
    public static void deserialize (SerializationStreamReader streamReader, ChannelName instance)
    {
    }

    public static ChannelName instantiate (SerializationStreamReader streamReader)
        throws SerializationException
    {        
        return new ChannelName(streamReader.readString(), streamReader.readInt());
    }

    public static void serialize (SerializationStreamWriter streamWriter, ChannelName name)
        throws SerializationException
    {
        streamWriter.writeString(name.toString());
        streamWriter.writeInt(name.getCreatorId());
    }
}
