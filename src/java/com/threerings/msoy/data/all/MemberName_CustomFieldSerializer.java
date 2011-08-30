//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import com.threerings.orth.data.OrthName;
import com.threerings.orth.data.OrthName_CustomFieldSerializer;

/**
 * Custom field serializer for {@link MemberName}.
 */
public final class MemberName_CustomFieldSerializer
{
    public static void serialize (SerializationStreamWriter streamWriter, MemberName name)
        throws SerializationException
    {
        OrthName_CustomFieldSerializer.serialize(streamWriter, name);
    }

    public static MemberName instantiate (SerializationStreamReader streamReader)
        throws SerializationException
    {
        OrthName name = OrthName_CustomFieldSerializer.instantiate(streamReader);
        return new MemberName(name.toString(), name.getId());
    }

    public static void deserialize (SerializationStreamReader streamReader, MemberName instance)
    {
    }
}
