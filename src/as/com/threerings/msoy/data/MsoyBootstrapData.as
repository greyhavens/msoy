//
// $Id$

package com.threerings.msoy.data {

import com.threerings.presents.net.BootstrapData;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Msoy bootstrap data.
 */
public class MsoyBootstrapData extends BootstrapData
{
    /** The token generated during authentication. */
    public var sessionToken :String;

    public function MsoyBootstrapData ()
    {
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        sessionToken = ins.readField(String) as String;
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error("Client may not writeObject");
    }
}
}
