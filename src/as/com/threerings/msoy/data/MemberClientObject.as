//
// $Id: $

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.presents.data.ClientObject;

/**
 * The tiny loader client for a member.
 */
public class MemberClientObject extends ClientObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>bodyOid</code> field. */
    public static const BODY_OID :String = "bodyOid";
    // AUTO-GENERATED: FIELDS END

    /** The oid of the {@link MemberObject} once it's loaded; zero until then. */
    public var bodyOid :int;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        bodyOid = ins.readInt();
    }
}
}
