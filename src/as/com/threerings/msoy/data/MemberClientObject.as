//
// $Id: $

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.data.all.MemberName;

/**
 * The tiny loader client for a member.
 */
public class MemberClientObject extends ClientObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>name</code> field. */
    public static const NAME :String = "name";

    /** The field name of the <code>bodyOid</code> field. */
    public static const BODY_OID :String = "bodyOid";
    // AUTO-GENERATED: FIELDS END

    /** The name of the member we're loading for. */
    public var name :MemberName;

    /** The oid of the {@link MemberObject} once it's loaded; zero until then. */
    public var bodyOid :int;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        name = MemberName(ins.readObject());
        bodyOid = ins.readInt();
    }
}
}
