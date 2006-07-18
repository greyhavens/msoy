//
// $Id$

package com.threerings.msoy.data {

import com.threerings.presents.net.AuthResponseData;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

/**
 * Extends the normal auth response data with MSOY-specific bits.
 */
public class MsoyAuthResponseData extends AuthResponseData
{
    /** A machine identifier to be assigned to this machine. */
    public var ident :String;

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(ident);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        ident = (ins.readField(String) as String);
    }
}
}
