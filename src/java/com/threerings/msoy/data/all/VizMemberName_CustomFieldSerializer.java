package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

public class VizMemberName_CustomFieldSerializer
{
    public static void serialize (SerializationStreamWriter streamWriter, VizMemberName name)
        throws SerializationException
    {
        MemberName_CustomFieldSerializer.serialize(streamWriter, name);
        streamWriter.writeObject(name.getPhoto());
    }

    public static VizMemberName instantiate (SerializationStreamReader streamReader)
        throws SerializationException
    {
        return new VizMemberName(streamReader.readString(), streamReader.readInt(),
            (MediaDesc)streamReader.readObject());
    }

    public static void deserialize (SerializationStreamReader streamReader, VizMemberName instance)
    {
    }
}
