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

    /** The field name of the <code>position</code> field. */
    public static const POSITION :String = "position";
    // AUTO-GENERATED: FIELDS END

    /** The oid of the {@link MemberObject} once it's loaded; zero until then. */
    public var bodyOid :int;

    /** Our position in the login queue. */
    public var position :int;

    public function get memobj () :MemberObject
    {
        return _memobj;
    }

    /** Called from {@link BodyLoader} when the time is right. */
    public function setMemberObject (obj :MemberObject) :void
    {
        if (_memobj != null) {
            throw new Error("Erp, my memobj has already been set!");
        }
        _memobj = obj;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        bodyOid = ins.readInt();
        position = ins.readInt();
    }

    /** Not part of streaming, set only when {@link BodyLoader} finishes. */
    protected var _memobj :MemberObject;
}
}
