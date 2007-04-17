//
// $Id$

package com.threerings.msoy.data.gwt;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import com.threerings.msoy.data.all.MemberName;

/**
 * Custom field serializer for {@link MemberName}.
 */
public final class MemberName_CustomFieldSerializer
{
    public static void deserialize (SerializationStreamReader streamReader, MemberName instance)
    {
    }

    public static MemberName instantiate (SerializationStreamReader streamReader)
        throws SerializationException
    {        
        return new MemberName(streamReader.readString(), streamReader.readInt());
    }

    public static void serialize (SerializationStreamWriter streamWriter, MemberName name)
        throws SerializationException
    {
        streamWriter.writeString(name.toString());
        streamWriter.writeInt(name.getMemberId());
    }
}
