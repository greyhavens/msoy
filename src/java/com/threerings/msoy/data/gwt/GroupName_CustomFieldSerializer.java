//
// $Id$

package com.threerings.msoy.data.gwt;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import com.threerings.msoy.data.all.GroupName;

/**
 * Custom field serializer for {@link GroupName}.
 */
public final class GroupName_CustomFieldSerializer
{
    public static void deserialize (SerializationStreamReader streamReader, GroupName instance)
    {
    }

    public static GroupName instantiate (SerializationStreamReader streamReader)
        throws SerializationException
    {        
        return new GroupName(streamReader.readString(), streamReader.readInt());
    }

    public static void serialize (SerializationStreamWriter streamWriter, GroupName name)
        throws SerializationException
    {
        streamWriter.writeString(name.toString());
        streamWriter.writeInt(name.getGroupId());
    }
}
